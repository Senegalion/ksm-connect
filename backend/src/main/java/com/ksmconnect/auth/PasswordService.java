package com.ksmconnect.auth;

import com.ksmconnect.common.error.UnauthorizedException;
import com.ksmconnect.config.KsmProperties;
import com.ksmconnect.security.UserSessionService;
import com.ksmconnect.user.EmailAddresses;
import com.ksmconnect.user.User;
import com.ksmconnect.user.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
class PasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService tokenService;
    private final UserSessionService sessionService;
    private final ApplicationEventPublisher events;
    private final KsmProperties properties;
    private final Clock clock;

    PasswordService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthTokenService tokenService,
                    UserSessionService sessionService, ApplicationEventPublisher events,
                    KsmProperties properties, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.sessionService = sessionService;
        this.events = events;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public void requestReset(String rawEmail) {
        userRepository.findByEmail(EmailAddresses.normalize(rawEmail)).ifPresent(user -> {
            String rawToken = tokenService.issue(user.getId(), AuthTokenPurpose.PASSWORD_RESET,
                    properties.auth().passwordResetTtl());
            events.publishEvent(new AuthEmailRequested(AuthEmailKind.PASSWORD_RESET, user.getEmail(),
                    user.getFirstName(), rawToken));
        });
    }

    @Transactional
    public void confirmReset(String rawToken, String newPassword) {
        User user = userRepository.findById(tokenService.consume(rawToken, AuthTokenPurpose.PASSWORD_RESET))
                .orElseThrow();
        applyNewPassword(user, newPassword);
        user.markEmailVerified(clock.instant());
        sessionService.terminateAllSessions(user.getId());
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId).orElseThrow();
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new UnauthorizedException("INVALID_CREDENTIALS", "Current password does not match");
        }
        applyNewPassword(user, newPassword);
        sessionService.terminateAllSessions(user.getId());
    }

    private void applyNewPassword(User user, String newPassword) {
        PasswordPolicy.validate(newPassword, user.getEmail());
        user.changePassword(passwordEncoder.encode(newPassword));
    }
}
