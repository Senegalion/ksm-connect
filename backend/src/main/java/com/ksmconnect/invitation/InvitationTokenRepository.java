package com.ksmconnect.invitation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface InvitationTokenRepository extends JpaRepository<InvitationToken, UUID> {

    Optional<InvitationToken> findByTokenHash(String tokenHash);

    Optional<InvitationToken> findByEventIdAndRevokedAtIsNull(UUID eventId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update InvitationToken t set t.revokedAt = :now where t.eventId = :eventId and t.revokedAt is null")
    int revokeActive(UUID eventId, Instant now);
}
