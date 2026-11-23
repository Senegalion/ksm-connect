package com.ksmconnect.invitation;

import com.ksmconnect.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invitation_tokens")
public class InvitationToken extends BaseEntity {

    @Column(nullable = false, updatable = false)
    private UUID eventId;

    @Column(nullable = false, updatable = false)
    private String tokenHash;

    private UUID createdBy;

    private Instant expiresAt;

    private Instant revokedAt;

    protected InvitationToken() {
    }

    InvitationToken(UUID eventId, String tokenHash, UUID createdBy, Instant expiresAt) {
        this.eventId = eventId;
        this.tokenHash = tokenHash;
        this.createdBy = createdBy;
        this.expiresAt = expiresAt;
    }

    boolean isValid(Instant now) {
        return revokedAt == null && (expiresAt == null || expiresAt.isAfter(now));
    }

    UUID getEventId() {
        return eventId;
    }

    Instant getExpiresAt() {
        return expiresAt;
    }
}
