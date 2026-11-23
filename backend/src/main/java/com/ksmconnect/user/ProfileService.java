package com.ksmconnect.user;

import com.ksmconnect.common.error.BusinessRuleException;
import com.ksmconnect.common.error.UnauthorizedException;
import com.ksmconnect.security.UserSessionService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
class ProfileService {

    private final UserRepository userRepository;
    private final PhoneNumberNormalizer phoneNumberNormalizer;
    private final PasswordEncoder passwordEncoder;
    private final AccountDeletionPolicy deletionPolicy;
    private final UserSessionService sessionService;

    ProfileService(UserRepository userRepository, PhoneNumberNormalizer phoneNumberNormalizer,
                   PasswordEncoder passwordEncoder, AccountDeletionPolicy deletionPolicy,
                   UserSessionService sessionService) {
        this.userRepository = userRepository;
        this.phoneNumberNormalizer = phoneNumberNormalizer;
        this.passwordEncoder = passwordEncoder;
        this.deletionPolicy = deletionPolicy;
        this.sessionService = sessionService;
    }

    @Transactional(readOnly = true)
    User get(UUID userId) {
        return userRepository.findById(userId).orElseThrow();
    }

    @Transactional
    User update(UUID userId, PersonName name, String rawPhoneNumber) {
        User user = get(userId);
        user.updateProfile(name, phoneNumberNormalizer.toE164(rawPhoneNumber));
        return user;
    }

    @Transactional
    void delete(UUID userId, String password) {
        User user = get(userId);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedException("INVALID_CREDENTIALS", "Password does not match");
        }
        if (!deletionPolicy.canDelete(userId)) {
            throw new BusinessRuleException("ACCOUNT_IN_COMPLETED_DRAW",
                    "Account takes part in a completed drawing that is still within its retention period");
        }
        userRepository.delete(user);
        userRepository.flush();
        sessionService.terminateAllSessions(userId);
    }
}
