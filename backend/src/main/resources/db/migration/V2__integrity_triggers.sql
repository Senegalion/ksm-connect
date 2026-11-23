CREATE FUNCTION enforce_event_status_transition() RETURNS trigger AS $$
BEGIN
    IF NEW.status = OLD.status THEN
        RETURN NEW;
    END IF;
    IF (OLD.status, NEW.status) IN (
        ('DRAFT', 'REGISTRATION_OPEN'),
        ('DRAFT', 'CANCELLED'),
        ('REGISTRATION_OPEN', 'REGISTRATION_CLOSED'),
        ('REGISTRATION_OPEN', 'CANCELLED'),
        ('REGISTRATION_CLOSED', 'REGISTRATION_OPEN'),
        ('REGISTRATION_CLOSED', 'DRAW_COMPLETED'),
        ('REGISTRATION_CLOSED', 'CANCELLED')
    ) THEN
        RETURN NEW;
    END IF;
    RAISE EXCEPTION 'invalid event status transition from % to %', OLD.status, NEW.status
        USING ERRCODE = 'check_violation';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER events_status_transition
    BEFORE UPDATE OF status ON events
    FOR EACH ROW EXECUTE FUNCTION enforce_event_status_transition();

CREATE FUNCTION protect_completed_draw() RETURNS trigger AS $$
BEGIN
    IF OLD.status = 'DRAW_COMPLETED' AND (
        NEW.draw_completed_at IS DISTINCT FROM OLD.draw_completed_at
        OR NEW.draw_mode IS DISTINCT FROM OLD.draw_mode
        OR NEW.draw_at IS DISTINCT FROM OLD.draw_at
    ) THEN
        RAISE EXCEPTION 'completed draw settings are immutable' USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER events_protect_completed_draw
    BEFORE UPDATE ON events
    FOR EACH ROW EXECUTE FUNCTION protect_completed_draw();

CREATE FUNCTION guard_assignment_insert() RETURNS trigger AS $$
DECLARE
    event_status VARCHAR(30);
    inactive_count INTEGER;
BEGIN
    SELECT status INTO event_status FROM events WHERE id = NEW.event_id;
    IF event_status IS DISTINCT FROM 'REGISTRATION_CLOSED' THEN
        RAISE EXCEPTION 'assignments can only be created for an event with closed registration'
            USING ERRCODE = 'check_violation';
    END IF;
    SELECT count(*) INTO inactive_count
    FROM event_participants
    WHERE id IN (NEW.giver_participant_id, NEW.recipient_participant_id)
      AND status <> 'ACTIVE';
    IF inactive_count > 0 THEN
        RAISE EXCEPTION 'assignments can only reference active participants' USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER assignments_guard_insert
    BEFORE INSERT ON assignments
    FOR EACH ROW EXECUTE FUNCTION guard_assignment_insert();

CREATE FUNCTION forbid_assignment_modification() RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'DELETE' AND current_setting('ksm.allow_assignment_purge', true) = 'on' THEN
        RETURN OLD;
    END IF;
    RAISE EXCEPTION 'assignments are immutable' USING ERRCODE = 'check_violation';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER assignments_immutable
    BEFORE UPDATE OR DELETE ON assignments
    FOR EACH ROW EXECUTE FUNCTION forbid_assignment_modification();

CREATE FUNCTION verify_draw_completeness() RETURNS trigger AS $$
DECLARE
    active_count INTEGER;
    assignment_count INTEGER;
    covered_givers INTEGER;
    covered_recipients INTEGER;
BEGIN
    SELECT count(*) INTO active_count
    FROM event_participants WHERE event_id = NEW.id AND status = 'ACTIVE';

    SELECT count(*),
           count(DISTINCT a.giver_participant_id) FILTER (WHERE g.status = 'ACTIVE'),
           count(DISTINCT a.recipient_participant_id) FILTER (WHERE r.status = 'ACTIVE')
    INTO assignment_count, covered_givers, covered_recipients
    FROM assignments a
    JOIN event_participants g ON g.id = a.giver_participant_id
    JOIN event_participants r ON r.id = a.recipient_participant_id
    WHERE a.event_id = NEW.id;

    IF active_count < 2
        OR assignment_count <> active_count
        OR covered_givers <> active_count
        OR covered_recipients <> active_count THEN
        RAISE EXCEPTION 'draw for event is incomplete or inconsistent' USING ERRCODE = 'check_violation';
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER events_draw_completeness
    AFTER UPDATE OF status ON events
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
    WHEN (NEW.status = 'DRAW_COMPLETED' AND OLD.status <> 'DRAW_COMPLETED')
    EXECUTE FUNCTION verify_draw_completeness();

CREATE FUNCTION guard_participant_change() RETURNS trigger AS $$
DECLARE
    event_status VARCHAR(30);
BEGIN
    SELECT status INTO event_status FROM events WHERE id = NEW.event_id;
    IF event_status IN ('DRAW_COMPLETED', 'CANCELLED') THEN
        RAISE EXCEPTION 'participants cannot change after the draw or cancellation'
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER event_participants_guard_change
    BEFORE INSERT OR UPDATE OF status, event_id, user_id ON event_participants
    FOR EACH ROW EXECUTE FUNCTION guard_participant_change();

CREATE TABLE spring_session (
    primary_id            CHAR(36)     NOT NULL,
    session_id            CHAR(36)     NOT NULL,
    creation_time         BIGINT       NOT NULL,
    last_access_time      BIGINT       NOT NULL,
    max_inactive_interval INTEGER      NOT NULL,
    expiry_time           BIGINT       NOT NULL,
    principal_name        VARCHAR(100),
    CONSTRAINT spring_session_pk PRIMARY KEY (primary_id)
);

CREATE UNIQUE INDEX spring_session_ix1 ON spring_session (session_id);
CREATE INDEX spring_session_ix2 ON spring_session (expiry_time);
CREATE INDEX spring_session_ix3 ON spring_session (principal_name);

CREATE TABLE spring_session_attributes (
    session_primary_id CHAR(36)     NOT NULL,
    attribute_name     VARCHAR(200) NOT NULL,
    attribute_bytes    BYTEA        NOT NULL,
    CONSTRAINT spring_session_attributes_pk PRIMARY KEY (session_primary_id, attribute_name),
    CONSTRAINT spring_session_attributes_fk FOREIGN KEY (session_primary_id) REFERENCES spring_session (primary_id) ON DELETE CASCADE
);
