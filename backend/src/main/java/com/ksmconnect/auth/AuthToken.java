package com.ksmconnect.auth;

import com.ksmconnect.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_tokens")
public class AuthToken extends BaseEntity {

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthTokenPurpose purpose;

    @Column(nullable = false)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant consumedAt;

    protected AuthToken() {
    }

    AuthToken(UUID userId, AuthTokenPurpose purpose, String tokenHash, Instant expiresAt) {
        this.userId = userId;
        this.purpose = purpose;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    boolean isUsable(AuthTokenPurpose expectedPurpose, Instant now) {
        return purpose == expectedPurpose && consumedAt == null && expiresAt.isAfter(now);
    }

    void consume(Instant now) {
        consumedAt = now;
    }

    UUID getUserId() {
        return userId;
    }
}
