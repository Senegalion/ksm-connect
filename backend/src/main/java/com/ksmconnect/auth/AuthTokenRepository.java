package com.ksmconnect.auth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

interface AuthTokenRepository extends JpaRepository<AuthToken, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AuthToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("delete from AuthToken t where t.userId = :userId and t.purpose = :purpose and t.consumedAt is null")
    void deleteActiveTokens(UUID userId, AuthTokenPurpose purpose);
}
