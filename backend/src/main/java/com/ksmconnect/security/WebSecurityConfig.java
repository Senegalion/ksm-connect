package com.ksmconnect.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.session.DisableEncodeUrlFilter;

import java.time.Duration;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class WebSecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/csrf",
            "/api/auth/session",
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/verify-email",
            "/api/auth/resend-verification",
            "/api/auth/password-reset/request",
            "/api/auth/password-reset/confirm",
            "/api/public/**"
    };

    private static final String[] DOCUMENTATION_ENDPOINTS = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            CsrfTokenRepository csrfTokenRepository,
                                            SecurityContextRepository securityContextRepository,
                                            RateLimiter rateLimiter,
                                            ClientIpResolver clientIpResolver) throws Exception {
        http
                .cors(cors -> {
                })
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository))
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fixation -> fixation.changeSessionId()))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
                        .deleteCookies(SessionCookieConfig.SESSION_COOKIE_NAME))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'none'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'"))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(Duration.ofDays(365).toSeconds()))
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .frameOptions(frame -> frame.deny())
                        .permissionsPolicyHeader(permissions -> permissions.policy("camera=(), microphone=(), geolocation=()")))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                ProblemResponseWriter.write(response, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED"))
                        .accessDeniedHandler((request, response, exception) ->
                                ProblemResponseWriter.write(response, HttpStatus.FORBIDDEN,
                                        exception instanceof CsrfException ? "CSRF_INVALID" : "FORBIDDEN")))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(DOCUMENTATION_ENDPOINTS).permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .addFilterAfter(new ApiRateLimitFilter(rateLimiter, clientIpResolver), DisableEncodeUrlFilter.class);
        return http.build();
    }
}
