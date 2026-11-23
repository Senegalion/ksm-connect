package com.ksmconnect.auth;

import com.ksmconnect.config.KsmProperties;
import com.ksmconnect.user.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Component
class LoginAttemptRecorder {

    private final UserRepository userRepository;
    private final KsmProperties properties;
    private final Clock clock;

    LoginAttemptRecorder(UserRepository userRepository, KsmProperties properties, Clock clock) {
        this.userRepository = userRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> user.recordFailedLogin(
                clock.instant(), properties.auth().maxFailedLogins(), properties.auth().lockoutDuration()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> user.recordSuccessfulLogin());
    }
}
