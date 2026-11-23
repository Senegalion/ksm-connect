package com.ksmconnect.auth;

import com.ksmconnect.common.error.BusinessRuleException;
import com.ksmconnect.common.token.SecureTokenGenerator;
import com.ksmconnect.common.token.TokenHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
class AuthTokenService {

    private final AuthTokenRepository repository;
    private final SecureTokenGenerator tokenGenerator;
    private final TokenHasher tokenHasher;
    private final Clock clock;

    AuthTokenService(AuthTokenRepository repository, SecureTokenGenerator tokenGenerator,
                     TokenHasher tokenHasher, Clock clock) {
        this.repository = repository;
        this.tokenGenerator = tokenGenerator;
        this.tokenHasher = tokenHasher;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    String issue(UUID userId, AuthTokenPurpose purpose, Duration ttl) {
        repository.deleteActiveTokens(userId, purpose);
        String rawToken = tokenGenerator.urlSafeToken();
        repository.save(new AuthToken(userId, purpose, tokenHasher.hash(rawToken), clock.instant().plus(ttl)));
        return rawToken;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    UUID consume(String rawToken, AuthTokenPurpose purpose) {
        Instant now = clock.instant();
        AuthToken token = repository.findByTokenHash(tokenHasher.hash(rawToken))
                .filter(candidate -> candidate.isUsable(purpose, now))
                .orElseThrow(() -> new BusinessRuleException("INVALID_OR_EXPIRED_TOKEN", "Token is invalid or expired"));
        token.consume(now);
        return token.getUserId();
    }
}
