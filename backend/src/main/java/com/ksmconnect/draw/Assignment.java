package com.ksmconnect.draw;

import com.ksmconnect.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

@Entity
@Immutable
@Table(name = "assignments")
class Assignment extends BaseEntity {

    @Column(nullable = false, updatable = false)
    private UUID eventId;

    @Column(nullable = false, updatable = false)
    private UUID giverParticipantId;

    @Column(nullable = false, updatable = false)
    private UUID recipientParticipantId;

    protected Assignment() {
    }

    Assignment(UUID eventId, UUID giverParticipantId, UUID recipientParticipantId) {
        this.eventId = eventId;
        this.giverParticipantId = giverParticipantId;
        this.recipientParticipantId = recipientParticipantId;
    }

    UUID getEventId() {
        return eventId;
    }

    UUID getGiverParticipantId() {
        return giverParticipantId;
    }

    UUID getRecipientParticipantId() {
        return recipientParticipantId;
    }

    @Override
    public String toString() {
        return "Assignment[redacted]";
    }
}
