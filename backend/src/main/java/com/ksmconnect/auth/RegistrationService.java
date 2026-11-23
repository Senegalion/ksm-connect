package com.ksmconnect.auth;

import com.ksmconnect.config.KsmProperties;
import com.ksmconnect.user.EmailAddresses;
import com.ksmconnect.user.PersonName;
import com.ksmconnect.user.PhoneNumberNormalizer;
import com.ksmconnect.user.User;
import com.ksmconnect.user.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

@Service
class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PhoneNumberNormalizer phoneNumberNormalizer;
    private final AuthTokenService tokenService;
    private final ApplicationEventPublisher events;
    private final KsmProperties properties;
    private final Clock clock;

    RegistrationService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        PhoneNumberNormalizer phoneNumberNormalizer, AuthTokenService tokenService,
                        ApplicationEventPublisher events, KsmProperties properties, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.phoneNumberNormalizer = phoneNumberNormalizer;
        this.tokenService = tokenService;
        this.events = events;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public void register(AuthRequests.Register request) {
        String email = EmailAddresses.normalize(request.email());
        PasswordPolicy.validate(request.password(), email);
        String phoneNumber = phoneNumberNormalizer.toE164(request.phoneNumber());
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            handleExistingAccount(existing.get());
            return;
        }
        User user = userRepository.saveAndFlush(new User(
                email,
                new PersonName(request.firstName(), request.lastName()),
                phoneNumber,
                passwordEncoder.encode(request.password()),
                properties.privacyNoticeVersion(),
                clock.instant()));
        sendVerification(user);
    }

    @Transactional
    public void resendVerification(String rawEmail) {
        userRepository.findByEmail(EmailAddresses.normalize(rawEmail))
                .filter(user -> !user.isEmailVerified())
                .ifPresent(this::sendVerification);
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        User user = userRepository.findById(tokenService.consume(rawToken, AuthTokenPurpose.EMAIL_VERIFICATION))
                .orElseThrow();
        user.markEmailVerified(clock.instant());
    }

    private void handleExistingAccount(User user) {
        if (user.isEmailVerified()) {
            events.publishEvent(new AuthEmailRequested(AuthEmailKind.ACCOUNT_ALREADY_EXISTS, user.getEmail(),
                    user.getFirstName(), null));
        } else {
            sendVerification(user);
        }
    }

    private void sendVerification(User user) {
        String rawToken = tokenService.issue(user.getId(), AuthTokenPurpose.EMAIL_VERIFICATION,
                properties.auth().emailVerificationTtl());
        events.publishEvent(new AuthEmailRequested(AuthEmailKind.EMAIL_VERIFICATION, user.getEmail(),
                user.getFirstName(), rawToken));
    }
}
