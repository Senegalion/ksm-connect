package com.ksmconnect.auth;

import com.ksmconnect.common.error.UnauthorizedException;
import com.ksmconnect.security.ClientIpResolver;
import com.ksmconnect.security.RateLimitPolicy;
import com.ksmconnect.security.RateLimiter;
import com.ksmconnect.user.EmailAddresses;
import com.ksmconnect.user.User;
import com.ksmconnect.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Optional;

@Service
class LoginService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final LoginAttemptRecorder attemptRecorder;
    private final SecurityContextRepository securityContextRepository;
    private final CsrfTokenRepository csrfTokenRepository;
    private final RateLimiter rateLimiter;
    private final ClientIpResolver clientIpResolver;
    private final Clock clock;
    private final SecurityContextHolderStrategy contextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

    LoginService(AuthenticationManager authenticationManager, UserRepository userRepository,
                 LoginAttemptRecorder attemptRecorder, SecurityContextRepository securityContextRepository,
                 CsrfTokenRepository csrfTokenRepository, RateLimiter rateLimiter,
                 ClientIpResolver clientIpResolver, Clock clock) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.attemptRecorder = attemptRecorder;
        this.securityContextRepository = securityContextRepository;
        this.csrfTokenRepository = csrfTokenRepository;
        this.rateLimiter = rateLimiter;
        this.clientIpResolver = clientIpResolver;
        this.clock = clock;
    }

    SessionView login(AuthRequests.Login request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String email = EmailAddresses.normalize(request.email());
        rateLimiter.consume(RateLimitPolicy.LOGIN_PER_IP, clientIpResolver.resolve(httpRequest));
        rateLimiter.consume(RateLimitPolicy.LOGIN_PER_ACCOUNT, email);

        Optional<User> account = userRepository.findByEmail(email);
        if (account.isPresent() && account.get().isLocked(clock.instant())) {
            throw invalidCredentials();
        }
        Authentication authentication = authenticate(email, request.password(), account);
        User user = account.orElseThrow(LoginService::invalidCredentials);
        if (!user.isEmailVerified()) {
            throw new UnauthorizedException("EMAIL_NOT_VERIFIED", "Email address has not been verified");
        }
        attemptRecorder.recordSuccess(user.getId());
        establishSession(authentication, httpRequest, httpResponse);
        return SessionView.of(user);
    }

    private Authentication authenticate(String email, String password, Optional<User> account) {
        try {
            return authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, password));
        } catch (AuthenticationException exception) {
            account.ifPresent(user -> attemptRecorder.recordFailure(user.getId()));
            throw invalidCredentials();
        }
    }

    private void establishSession(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContext context = contextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        contextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        csrfTokenRepository.saveToken(csrfTokenRepository.generateToken(request), request, response);
    }

    private static UnauthorizedException invalidCredentials() {
        return new UnauthorizedException("INVALID_CREDENTIALS", "Invalid credentials");
    }
}
