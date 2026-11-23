package com.ksmconnect.notification;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
class OutboxClaimer {

    private static final String CLAIM_SQL = """
            UPDATE notification_outbox
            SET status = 'PROCESSING',
                locked_until = :lockedUntil,
                attempt_count = attempt_count + 1,
                updated_at = :now,
                version = version + 1
            WHERE id IN (
                SELECT id FROM notification_outbox
                WHERE (status IN ('PENDING', 'RETRY_PENDING') AND next_attempt_at <= :now)
                   OR (status = 'PROCESSING' AND locked_until < :now)
                ORDER BY next_attempt_at
                LIMIT :limit
                FOR UPDATE SKIP LOCKED
            )
            RETURNING id
            """;

    private final JdbcClient jdbcClient;

    OutboxClaimer(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Transactional
    List<UUID> claim(Instant now, Duration processingTimeout, int limit) {
        return jdbcClient.sql(CLAIM_SQL)
                .param("now", Timestamp.from(now))
                .param("lockedUntil", Timestamp.from(now.plus(processingTimeout)))
                .param("limit", limit)
                .query(UUID.class)
                .list();
    }
}
