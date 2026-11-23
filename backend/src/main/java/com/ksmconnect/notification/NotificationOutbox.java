package com.ksmconnect.notification;

import com.ksmconnect.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_outbox")
public class NotificationOutbox extends BaseEntity {

    private static final int MAX_ERROR_LENGTH = 200;

    @Column(nullable = false, updatable = false)
    private UUID eventId;

    @Column(nullable = false, updatable = false)
    private UUID participantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    @Column(nullable = false, updatable = false)
    private String idempotencyKey;

    @Column(nullable = false)
    private int attemptCount;

    @Column(nullable = false)
    private int maxAttempts;

    @Column(nullable = false)
    private Instant nextAttemptAt;

    private Instant lockedUntil;

    private String lastError;

    private String providerMessageId;

    private Instant sentAt;

    protected NotificationOutbox() {
    }

    NotificationOutbox(UUID eventId, UUID participantId, NotificationChannel channel, NotificationType type,
                       int maxAttempts, Instant now) {
        this.eventId = eventId;
        this.participantId = participantId;
        this.channel = channel;
        this.type = type;
        this.status = NotificationStatus.PENDING;
        this.idempotencyKey = type + ":" + participantId + ":" + channel;
        this.maxAttempts = maxAttempts;
        this.nextAttemptAt = now;
    }

    void markSent(Instant now, String providerMessageId) {
        this.status = NotificationStatus.SENT;
        this.sentAt = now;
        this.providerMessageId = truncate(providerMessageId);
        this.lockedUntil = null;
        this.lastError = null;
    }

    void markAttemptFailed(String error, boolean permanent, Instant nextAttemptAt) {
        this.lastError = truncate(error);
        this.lockedUntil = null;
        if (permanent || attemptCount >= maxAttempts) {
            this.status = NotificationStatus.FAILED;
        } else {
            this.status = NotificationStatus.RETRY_PENDING;
            this.nextAttemptAt = nextAttemptAt;
        }
    }

    boolean requeue(Instant now) {
        if (status != NotificationStatus.FAILED && status != NotificationStatus.RETRY_PENDING) {
            return false;
        }
        this.status = NotificationStatus.RETRY_PENDING;
        this.attemptCount = 0;
        this.nextAttemptAt = now;
        return true;
    }

    boolean isProcessing() {
        return status == NotificationStatus.PROCESSING;
    }

    private static String truncate(String value) {
        return value == null || value.length() <= MAX_ERROR_LENGTH ? value : value.substring(0, MAX_ERROR_LENGTH);
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getParticipantId() {
        return participantId;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public NotificationType getType() {
        return type;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
