CREATE TABLE users (
    id                    UUID PRIMARY KEY,
    email                 VARCHAR(254) NOT NULL,
    first_name            VARCHAR(100) NOT NULL,
    last_name             VARCHAR(100) NOT NULL,
    phone_number          VARCHAR(16),
    password_hash         VARCHAR(255) NOT NULL,
    email_verified_at     TIMESTAMPTZ,
    failed_login_attempts INTEGER      NOT NULL DEFAULT 0,
    locked_until          TIMESTAMPTZ,
    privacy_notice_version VARCHAR(20) NOT NULL,
    privacy_accepted_at   TIMESTAMPTZ  NOT NULL,
    created_at            TIMESTAMPTZ  NOT NULL,
    updated_at            TIMESTAMPTZ  NOT NULL,
    version               BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT users_email_lowercase CHECK (email = lower(email)),
    CONSTRAINT users_phone_e164 CHECK (phone_number IS NULL OR phone_number ~ '^\+[1-9][0-9]{6,14}$'),
    CONSTRAINT users_failed_attempts_non_negative CHECK (failed_login_attempts >= 0)
);

CREATE UNIQUE INDEX users_email_uq ON users (email);

CREATE TABLE user_roles (
    user_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role    VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT user_roles_role_valid CHECK (role IN ('ADMIN', 'PARTICIPANT'))
);

CREATE TABLE auth_tokens (
    id          UUID PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    purpose     VARCHAR(30) NOT NULL,
    token_hash  VARCHAR(64) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    version     BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT auth_tokens_purpose_valid CHECK (purpose IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET'))
);

CREATE UNIQUE INDEX auth_tokens_hash_uq ON auth_tokens (token_hash);
CREATE INDEX auth_tokens_user_purpose_idx ON auth_tokens (user_id, purpose);

CREATE TABLE events (
    id                     UUID PRIMARY KEY,
    name                   VARCHAR(150)  NOT NULL,
    description            VARCHAR(4000),
    location               VARCHAR(200),
    starts_at              TIMESTAMPTZ,
    timezone               VARCHAR(64)   NOT NULL,
    registration_opens_at  TIMESTAMPTZ,
    registration_closes_at TIMESTAMPTZ,
    draw_mode              VARCHAR(20)   NOT NULL,
    draw_at                TIMESTAMPTZ,
    wishlist_deadline      TIMESTAMPTZ,
    participant_limit      INTEGER,
    status                 VARCHAR(30)   NOT NULL,
    draw_completed_at      TIMESTAMPTZ,
    cancelled_at           TIMESTAMPTZ,
    personal_data_purged_at TIMESTAMPTZ,
    created_by             UUID REFERENCES users (id) ON DELETE SET NULL,
    created_at             TIMESTAMPTZ   NOT NULL,
    updated_at             TIMESTAMPTZ   NOT NULL,
    version                BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT events_draw_mode_valid CHECK (draw_mode IN ('MANUAL', 'SCHEDULED')),
    CONSTRAINT events_status_valid CHECK (status IN ('DRAFT', 'REGISTRATION_OPEN', 'REGISTRATION_CLOSED', 'DRAW_COMPLETED', 'CANCELLED')),
    CONSTRAINT events_scheduled_requires_draw_at CHECK (draw_mode <> 'SCHEDULED' OR draw_at IS NOT NULL),
    CONSTRAINT events_registration_window_valid CHECK (registration_opens_at IS NULL OR registration_closes_at IS NULL OR registration_closes_at > registration_opens_at),
    CONSTRAINT events_draw_after_registration CHECK (draw_at IS NULL OR registration_closes_at IS NULL OR draw_at >= registration_closes_at),
    CONSTRAINT events_participant_limit_valid CHECK (participant_limit IS NULL OR participant_limit BETWEEN 2 AND 10000),
    CONSTRAINT events_draw_completed_consistent CHECK ((status = 'DRAW_COMPLETED') = (draw_completed_at IS NOT NULL)),
    CONSTRAINT events_cancelled_consistent CHECK ((status = 'CANCELLED') = (cancelled_at IS NOT NULL))
);

CREATE INDEX events_status_draw_at_idx ON events (status, draw_at);
CREATE INDEX events_status_registration_closes_idx ON events (status, registration_closes_at);

CREATE TABLE invitation_tokens (
    id         UUID PRIMARY KEY,
    event_id   UUID        NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    created_by UUID REFERENCES users (id) ON DELETE SET NULL,
    expires_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version    BIGINT      NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX invitation_tokens_hash_uq ON invitation_tokens (token_hash);
CREATE UNIQUE INDEX invitation_tokens_one_active_per_event_uq ON invitation_tokens (event_id) WHERE revoked_at IS NULL;

CREATE TABLE event_participants (
    id           UUID PRIMARY KEY,
    event_id     UUID        NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    user_id      UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status       VARCHAR(20) NOT NULL,
    joined_at    TIMESTAMPTZ NOT NULL,
    withdrawn_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,
    version      BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT event_participants_status_valid CHECK (status IN ('ACTIVE', 'WITHDRAWN')),
    CONSTRAINT event_participants_withdrawn_consistent CHECK ((status = 'WITHDRAWN') = (withdrawn_at IS NOT NULL)),
    CONSTRAINT event_participants_event_user_uq UNIQUE (event_id, user_id),
    CONSTRAINT event_participants_event_id_uq UNIQUE (event_id, id)
);

CREATE INDEX event_participants_user_idx ON event_participants (user_id);

CREATE TABLE gift_suggestions (
    id                   UUID PRIMARY KEY,
    event_participant_id UUID          NOT NULL REFERENCES event_participants (id) ON DELETE CASCADE,
    position             SMALLINT      NOT NULL,
    type                 VARCHAR(20)   NOT NULL,
    url                  VARCHAR(2048),
    description          VARCHAR(500),
    created_at           TIMESTAMPTZ   NOT NULL,
    updated_at           TIMESTAMPTZ   NOT NULL,
    version              BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT gift_suggestions_position_valid CHECK (position BETWEEN 1 AND 3),
    CONSTRAINT gift_suggestions_type_valid CHECK (type IN ('URL', 'DESCRIPTION')),
    CONSTRAINT gift_suggestions_content_matches_type CHECK (
        (type = 'URL' AND url IS NOT NULL AND description IS NULL AND url ~* '^https?://[^\s]+$')
        OR (type = 'DESCRIPTION' AND description IS NOT NULL AND url IS NULL AND length(btrim(description)) > 0)
    ),
    CONSTRAINT gift_suggestions_participant_position_uq UNIQUE (event_participant_id, position)
);

CREATE TABLE assignments (
    id                       UUID PRIMARY KEY,
    event_id                 UUID        NOT NULL,
    giver_participant_id     UUID        NOT NULL,
    recipient_participant_id UUID        NOT NULL,
    created_at               TIMESTAMPTZ NOT NULL,
    updated_at               TIMESTAMPTZ NOT NULL,
    version                  BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT assignments_event_fk FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE RESTRICT,
    CONSTRAINT assignments_giver_fk FOREIGN KEY (event_id, giver_participant_id) REFERENCES event_participants (event_id, id) ON DELETE RESTRICT,
    CONSTRAINT assignments_recipient_fk FOREIGN KEY (event_id, recipient_participant_id) REFERENCES event_participants (event_id, id) ON DELETE RESTRICT,
    CONSTRAINT assignments_no_self_assignment CHECK (giver_participant_id <> recipient_participant_id),
    CONSTRAINT assignments_event_giver_uq UNIQUE (event_id, giver_participant_id),
    CONSTRAINT assignments_event_recipient_uq UNIQUE (event_id, recipient_participant_id)
);

CREATE TABLE notification_outbox (
    id                  UUID PRIMARY KEY,
    event_id            UUID         NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    participant_id      UUID         NOT NULL REFERENCES event_participants (id) ON DELETE CASCADE,
    channel             VARCHAR(10)  NOT NULL,
    type                VARCHAR(40)  NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    idempotency_key     VARCHAR(120) NOT NULL,
    attempt_count       INTEGER      NOT NULL DEFAULT 0,
    max_attempts        INTEGER      NOT NULL,
    next_attempt_at     TIMESTAMPTZ  NOT NULL,
    locked_until        TIMESTAMPTZ,
    last_error          VARCHAR(200),
    provider_message_id VARCHAR(200),
    sent_at             TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    version             BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT notification_outbox_channel_valid CHECK (channel IN ('EMAIL', 'SMS')),
    CONSTRAINT notification_outbox_type_valid CHECK (type IN ('ASSIGNMENT_READY')),
    CONSTRAINT notification_outbox_status_valid CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'RETRY_PENDING', 'FAILED')),
    CONSTRAINT notification_outbox_attempts_valid CHECK (attempt_count >= 0 AND max_attempts > 0),
    CONSTRAINT notification_outbox_sent_consistent CHECK ((status = 'SENT') = (sent_at IS NOT NULL))
);

CREATE UNIQUE INDEX notification_outbox_idempotency_uq ON notification_outbox (idempotency_key);
CREATE INDEX notification_outbox_due_idx ON notification_outbox (status, next_attempt_at);
CREATE INDEX notification_outbox_event_idx ON notification_outbox (event_id);

CREATE TABLE audit_events (
    id            UUID PRIMARY KEY,
    type          VARCHAR(60) NOT NULL,
    actor_user_id UUID,
    event_id      UUID,
    details       VARCHAR(500),
    created_at    TIMESTAMPTZ NOT NULL
);

CREATE INDEX audit_events_event_idx ON audit_events (event_id, created_at);
