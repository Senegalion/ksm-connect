package com.ksmconnect.auth;

import com.ksmconnect.security.AuthenticatedUser;
import com.ksmconnect.security.ClientIpResolver;
import com.ksmconnect.security.CurrentUser;
import com.ksmconnect.security.RateLimitPolicy;
import com.ksmconnect.security.RateLimiter;
import com.ksmconnect.user.EmailAddresses;
import com.ksmconnect.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final RegistrationService registrationService;
    private final PasswordService passwordService;
    private final LoginService loginService;
    private final UserRepository userRepository;
    private final RateLimiter rateLimiter;
    private final ClientIpResolver clientIpResolver;

    AuthController(RegistrationService registrationService, PasswordService passwordService,
                   LoginService loginService, UserRepository userRepository, RateLimiter rateLimiter,
                   ClientIpResolver clientIpResolver) {
        this.registrationService = registrationService;
        this.passwordService = passwordService;
        this.loginService = loginService;
        this.userRepository = userRepository;
        this.rateLimiter = rateLimiter;
        this.clientIpResolver = clientIpResolver;
    }

    record CsrfView(String headerName, String token) {
    }

    @GetMapping("/csrf")
    CsrfView csrf(CsrfToken token) {
        return new CsrfView(token.getHeaderName(), token.getToken());
    }

    @GetMapping("/session")
    SessionView session(@CurrentUser AuthenticatedUser principal) {
        if (principal == null) {
            return SessionView.anonymous();
        }
        return userRepository.findById(principal.id()).map(SessionView::of).orElseGet(SessionView::anonymous);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void register(@Valid @RequestBody AuthRequests.Register request, HttpServletRequest httpRequest) {
        rateLimiter.consume(RateLimitPolicy.REGISTRATION_PER_IP, clientIpResolver.resolve(httpRequest));
        rateLimiter.consume(RateLimitPolicy.EMAIL_REQUEST_PER_ACCOUNT, EmailAddresses.normalize(request.email()));
        try {
            registrationService.register(request);
        } catch (DataIntegrityViolationException concurrentRegistration) {
            registrationService.resendVerification(request.email());
        }
    }

    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void verifyEmail(@Valid @RequestBody AuthRequests.TokenConfirmation request) {
        registrationService.verifyEmail(request.token());
    }

    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void resendVerification(@Valid @RequestBody AuthRequests.EmailOnly request, HttpServletRequest httpRequest) {
        limitEmailRequest(request.email(), httpRequest);
        registrationService.resendVerification(request.email());
    }

    @PostMapping("/password-reset/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void requestPasswordReset(@Valid @RequestBody AuthRequests.EmailOnly request, HttpServletRequest httpRequest) {
        limitEmailRequest(request.email(), httpRequest);
        passwordService.requestReset(request.email());
    }

    @PostMapping("/password-reset/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void confirmPasswordReset(@Valid @RequestBody AuthRequests.PasswordResetConfirmation request,
                              HttpServletRequest httpRequest) {
        rateLimiter.consume(RateLimitPolicy.EMAIL_REQUEST_PER_IP, clientIpResolver.resolve(httpRequest));
        passwordService.confirmReset(request.token(), request.newPassword());
    }

    @PostMapping("/login")
    SessionView login(@Valid @RequestBody AuthRequests.Login request, HttpServletRequest httpRequest,
                      HttpServletResponse httpResponse) {
        return loginService.login(request, httpRequest, httpResponse);
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changePassword(@CurrentUser AuthenticatedUser principal,
                        @Valid @RequestBody AuthRequests.PasswordChange request,
                        HttpServletRequest httpRequest) {
        rateLimiter.consume(RateLimitPolicy.LOGIN_PER_ACCOUNT, principal.id().toString());
        passwordService.changePassword(principal.id(), request.currentPassword(), request.newPassword());
        httpRequest.getSession().invalidate();
    }

    private void limitEmailRequest(String email, HttpServletRequest httpRequest) {
        rateLimiter.consume(RateLimitPolicy.EMAIL_REQUEST_PER_IP, clientIpResolver.resolve(httpRequest));
        rateLimiter.consume(RateLimitPolicy.EMAIL_REQUEST_PER_ACCOUNT, EmailAddresses.normalize(email));
    }
}
