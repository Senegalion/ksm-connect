package com.ksmconnect.auth;

import com.ksmconnect.support.IntegrationTest;
import com.ksmconnect.support.RecordingEmailSender;
import com.ksmconnect.support.TestApi;
import com.ksmconnect.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class AuthFlowIT {

    @Autowired
    TestApi api;
    @Autowired
    MockMvc mvc;
    @Autowired
    RecordingEmailSender mail;
    @Autowired
    UserRepository userRepository;
    @Autowired
    JdbcClient jdbc;

    @Test
    void registrationRequiresEmailVerificationBeforeLogin() throws Exception {
        String email = TestApi.uniqueEmail("anna");
        api.register(email, "Anna", "Kowalska");

        api.attemptLogin(email, TestApi.PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));

        api.verify(email);
        TestApi.Session session = api.login(email);
        api.get(session, "/api/auth/session")
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.phoneNumber").value("+48600100200"))
                .andExpect(jsonPath("$.user.roles[0]").value("PARTICIPANT"));
    }

    @Test
    void storesArgon2HashAndOnlyHashedTokens() throws Exception {
        String email = TestApi.uniqueEmail("hash");
        api.register(email, "Hanna", "Hash");
        String token = api.awaitToken(email, "Potwierdź");

        String passwordHash = userRepository.findByEmail(email).orElseThrow().getPasswordHash();
        assertThat(passwordHash).startsWith("$argon2id$").doesNotContain(TestApi.PASSWORD);
        Integer rawTokenRows = jdbc.sql("SELECT count(*) FROM auth_tokens WHERE token_hash = :token")
                .param("token", token).query(Integer.class).single();
        assertThat(rawTokenRows).isZero();
    }

    @Test
    void verificationTokenIsSingleUse() throws Exception {
        String email = TestApi.uniqueEmail("single");
        api.register(email, "Jan", "Nowak");
        String token = api.awaitToken(email, "Potwierdź");
        verifyWith(token).andExpect(status().isNoContent());
        verifyWith(token)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_OR_EXPIRED_TOKEN"));
        verifyWith("forged-token-value")
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void duplicateRegistrationDoesNotRevealOrCreateAccount() throws Exception {
        String email = TestApi.uniqueEmail("dup");
        api.register(email, "Ewa", "Pierwsza");
        api.verify(email);
        api.register(email.toUpperCase(), "Ewa", "Druga");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(mail.sentTo(email)).anyMatch(message -> message.subject().contains("Masz już konto")));
        assertThat(userRepository.findByEmail(email).orElseThrow().getLastName()).isEqualTo("Pierwsza");
    }

    @Test
    void wrongPasswordIsRejectedGenericallyAndLocksAccount() throws Exception {
        String email = TestApi.uniqueEmail("lock");
        api.register(email, "Piotr", "Zamek");
        api.verify(email);

        for (int i = 0; i < 5; i++) {
            api.attemptLogin(email, "zle haslo " + i)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }
        api.attemptLogin(email, TestApi.PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        api.attemptLogin("nobody-" + email, TestApi.PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void passwordResetChangesPasswordAndTerminatesExistingSessions() throws Exception {
        TestApi.Session session = api.participant("Reset");
        mvc.perform(post("/api/auth/password-reset/request").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(api.toJson(Map.of("email", session.email()))))
                .andExpect(status().isAccepted());
        String token = api.awaitToken(session.email(), "Ustaw nowe hasło");

        mvc.perform(post("/api/auth/password-reset/confirm").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(api.toJson(Map.of("token", token, "newPassword", "nowe zimowe haslo 2026"))))
                .andExpect(status().isNoContent());

        api.get(session, "/api/me").andExpect(status().isUnauthorized());
        api.attemptLogin(session.email(), TestApi.PASSWORD).andExpect(status().isUnauthorized());
        api.attemptLogin(session.email(), "nowe zimowe haslo 2026").andExpect(status().isOk());
    }

    @Test
    void passwordResetForUnknownEmailLooksIdentical() throws Exception {
        mvc.perform(post("/api/auth/password-reset/request").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(api.toJson(Map.of("email", TestApi.uniqueEmail("ghost")))))
                .andExpect(status().isAccepted());
    }

    @Test
    void logoutInvalidatesSession() throws Exception {
        TestApi.Session session = api.participant("Logout");
        api.post(session, "/api/auth/logout", null).andExpect(status().isNoContent());
        api.get(session, "/api/me").andExpect(status().isUnauthorized());
    }

    @Test
    void stateChangingRequestsRequireCsrfToken() throws Exception {
        TestApi.Session session = api.participant("Csrf");
        mvc.perform(post("/api/auth/logout").cookie(session.cookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void csrfEndpointIssuesUsableToken() throws Exception {
        mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void sessionCookieIsHttpOnlyAndSameSite() throws Exception {
        String email = TestApi.uniqueEmail("cookie");
        api.register(email, "Ciastek", "Test");
        api.verify(email);
        api.attemptLogin(email, TestApi.PASSWORD)
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")));
    }

    @Test
    void anonymousSessionReportsUnauthenticated() throws Exception {
        mvc.perform(get("/api/auth/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    void registrationValidatesInput() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(api.toJson(Map.of("email", "not-an-email", "password", "short",
                                "firstName", "", "lastName", "X", "phoneNumber", "1", "privacyNoticeAccepted", false))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(4));
    }

    @Test
    void securityHeadersArePresent() throws Exception {
        mvc.perform(get("/api/auth/session"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().exists("Content-Security-Policy"));
    }

    @Test
    void healthEndpointIsPublicButDetailsAreHidden() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
        mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
    }

    private ResultActions verifyWith(String token) throws Exception {
        return mvc.perform(post("/api/auth/verify-email").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(api.toJson(Map.of("token", token))));
    }
}
