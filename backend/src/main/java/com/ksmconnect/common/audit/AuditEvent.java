package com.ksmconnect.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String type;

    private UUID actorUserId;

    private UUID eventId;

    private String details;

    @Column(nullable = false)
    private Instant createdAt;

    protected AuditEvent() {
    }

    AuditEvent(String type, UUID actorUserId, UUID eventId, String details, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.actorUserId = actorUserId;
        this.eventId = eventId;
        this.details = details;
        this.createdAt = createdAt;
    }

    public String getType() {
        return type;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getDetails() {
        return details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
