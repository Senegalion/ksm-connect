package com.ksmconnect.auth;

import com.ksmconnect.common.audit.AuditLog;
import com.ksmconnect.common.audit.AuditType;
import com.ksmconnect.security.UserSessionService;
import com.ksmconnect.user.EmailAddresses;
import com.ksmconnect.user.Role;
import com.ksmconnect.user.User;
import com.ksmconnect.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminPromotionTask {

    private final UserRepository userRepository;
    private final UserSessionService sessionService;
    private final AuditLog auditLog;

    AdminPromotionTask(UserRepository userRepository, UserSessionService sessionService, AuditLog auditLog) {
        this.userRepository = userRepository;
        this.sessionService = sessionService;
        this.auditLog = auditLog;
    }

    @Transactional
    public User promote(String rawEmail) {
        User user = userRepository.findByEmail(EmailAddresses.normalize(rawEmail))
                .orElseThrow(() -> new IllegalArgumentException("No account exists for the given email"));
        if (!user.isEmailVerified()) {
            throw new IllegalStateException("The account must verify its email address before becoming an administrator");
        }
        user.grantRole(Role.ADMIN);
        sessionService.terminateAllSessions(user.getId());
        auditLog.record(AuditType.ADMIN_ROLE_GRANTED, null, null, "user=" + user.getId());
        return user;
    }
}
