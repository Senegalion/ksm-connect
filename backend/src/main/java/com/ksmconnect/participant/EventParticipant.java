package com.ksmconnect.participant;

import com.ksmconnect.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "event_participants")
public class EventParticipant extends BaseEntity {

    @Column(nullable = false, updatable = false)
    private UUID eventId;

    @Column(nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipantStatus status;

    @Column(nullable = false)
    private Instant joinedAt;

    private Instant withdrawnAt;

    protected EventParticipant() {
    }

    EventParticipant(UUID eventId, UUID userId, Instant joinedAt) {
        this.eventId = eventId;
        this.userId = userId;
        this.status = ParticipantStatus.ACTIVE;
        this.joinedAt = joinedAt;
    }

    void withdraw(Instant now) {
        status = ParticipantStatus.WITHDRAWN;
        withdrawnAt = now;
    }

    void rejoin(Instant now) {
        status = ParticipantStatus.ACTIVE;
        withdrawnAt = null;
        joinedAt = now;
    }

    public boolean isActive() {
        return status == ParticipantStatus.ACTIVE;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getUserId() {
        return userId;
    }

    public ParticipantStatus getStatus() {
        return status;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }
}
