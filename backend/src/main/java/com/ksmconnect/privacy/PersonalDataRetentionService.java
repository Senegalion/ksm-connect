package com.ksmconnect.privacy;

import com.ksmconnect.common.audit.AuditLog;
import com.ksmconnect.common.audit.AuditType;
import com.ksmconnect.config.KsmProperties;
import com.ksmconnect.event.EventRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PersonalDataRetentionService {

    private static final Duration UNVERIFIED_ACCOUNT_RETENTION = Duration.ofDays(30);
    private static final Duration EXPIRED_TOKEN_RETENTION = Duration.ofDays(7);

    private final EventRepository eventRepository;
    private final JdbcClient jdbcClient;
    private final AuditLog auditLog;
    private final KsmProperties properties;
    private final Clock clock;

    PersonalDataRetentionService(EventRepository eventRepository, JdbcClient jdbcClient, AuditLog auditLog,
                                 KsmProperties properties, Clock clock) {
        this.eventRepository = eventRepository;
        this.jdbcClient = jdbcClient;
        this.auditLog = auditLog;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<UUID> eventsDueForPurge() {
        Instant threshold = clock.instant().minus(Duration.ofDays(properties.retention().daysAfterEvent()));
        return eventRepository.findIdsDueForPurge(threshold);
    }

    @Transactional
    public void purgeEvent(UUID eventId) {
        jdbcClient.sql("SELECT set_config('ksm.allow_assignment_purge', 'on', true)").query().singleValue();
        jdbcClient.sql("DELETE FROM notification_outbox WHERE event_id = :id").param("id", eventId).update();
        jdbcClient.sql("DELETE FROM assignments WHERE event_id = :id").param("id", eventId).update();
        int participants = jdbcClient.sql("DELETE FROM event_participants WHERE event_id = :id").param("id", eventId).update();
        jdbcClient.sql("DELETE FROM invitation_tokens WHERE event_id = :id").param("id", eventId).update();
        jdbcClient.sql("UPDATE events SET personal_data_purged_at = :now, updated_at = :now WHERE id = :id")
                .param("now", Timestamp.from(clock.instant()))
                .param("id", eventId)
                .update();
        auditLog.record(AuditType.PERSONAL_DATA_PURGED, null, eventId, "participants=" + participants);
    }

    @Transactional
    public int purgeStaleAccountsAndTokens() {
        Instant now = clock.instant();
        jdbcClient.sql("DELETE FROM auth_tokens WHERE expires_at < :threshold")
                .param("threshold", Timestamp.from(now.minus(EXPIRED_TOKEN_RETENTION)))
                .update();
        return jdbcClient.sql("DELETE FROM users WHERE email_verified_at IS NULL AND created_at < :threshold")
                .param("threshold", Timestamp.from(now.minus(UNVERIFIED_ACCOUNT_RETENTION)))
                .update();
    }
}
