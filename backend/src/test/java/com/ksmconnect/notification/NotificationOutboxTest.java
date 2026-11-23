package com.ksmconnect.notification;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationOutboxTest {

    private static final Instant NOW = Instant.parse("2026-12-10T18:00:00Z");

    @Test
    void startsPendingWithDeterministicIdempotencyKey() {
        UUID participantId = UUID.randomUUID();
        NotificationOutbox entry = entry(participantId, 3);
        assertThat(entry.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(ReflectionTestUtils.getField(entry, "idempotencyKey"))
                .isEqualTo("ASSIGNMENT_READY:" + participantId + ":EMAIL");
    }

    @Test
    void transientFailureSchedulesRetryUntilAttemptsExhausted() {
        NotificationOutbox entry = entry(UUID.randomUUID(), 2);
        ReflectionTestUtils.setField(entry, "attemptCount", 1);
        entry.markAttemptFailed("EMAIL_SEND_FAILED", false, NOW.plusSeconds(30));
        assertThat(entry.getStatus()).isEqualTo(NotificationStatus.RETRY_PENDING);
        assertThat(entry.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(30));

        ReflectionTestUtils.setField(entry, "attemptCount", 2);
        entry.markAttemptFailed("EMAIL_SEND_FAILED", false, NOW.plusSeconds(60));
        assertThat(entry.getStatus()).isEqualTo(NotificationStatus.FAILED);
    }

    @Test
    void permanentFailureFailsImmediately() {
        NotificationOutbox entry = entry(UUID.randomUUID(), 8);
        ReflectionTestUtils.setField(entry, "attemptCount", 1);
        entry.markAttemptFailed("EMAIL_INVALID_MESSAGE", true, NOW);
        assertThat(entry.getStatus()).isEqualTo(NotificationStatus.FAILED);
    }

    @Test
    void requeueResetsAttemptsOnlyForFailedOrRetryPending() {
        NotificationOutbox entry = entry(UUID.randomUUID(), 1);
        assertThat(entry.requeue(NOW)).isFalse();
        ReflectionTestUtils.setField(entry, "attemptCount", 1);
        entry.markAttemptFailed("X", false, NOW);
        assertThat(entry.requeue(NOW)).isTrue();
        assertThat(entry.getStatus()).isEqualTo(NotificationStatus.RETRY_PENDING);
        assertThat(entry.getAttemptCount()).isZero();
        entry.markSent(NOW, "id");
        assertThat(entry.requeue(NOW)).isFalse();
    }

    @Test
    void truncatesLongErrorCodes() {
        NotificationOutbox entry = entry(UUID.randomUUID(), 1);
        ReflectionTestUtils.setField(entry, "attemptCount", 1);
        entry.markAttemptFailed("E".repeat(500), false, NOW);
        assertThat(entry.getLastError()).hasSize(200);
    }

    private static NotificationOutbox entry(UUID participantId, int maxAttempts) {
        return new NotificationOutbox(UUID.randomUUID(), participantId, NotificationChannel.EMAIL,
                NotificationType.ASSIGNMENT_READY, maxAttempts, NOW);
    }
}
