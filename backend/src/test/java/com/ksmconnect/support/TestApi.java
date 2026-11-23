package com.ksmconnect.support;

import com.jayway.jsonpath.JsonPath;
import com.ksmconnect.auth.AdminPromotionTask;
import jakarta.servlet.http.Cookie;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Component
public class TestApi {

    public static final String PASSWORD = "bezpieczne haslo wigilijne";
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    private final MockMvc mvc;
    private final RecordingEmailSender mail;
    private final AdminPromotionTask adminPromotionTask;
    private final JsonMapper json = JsonMapper.builder().build();

    public TestApi(MockMvc mvc, RecordingEmailSender mail, AdminPromotionTask adminPromotionTask) {
        this.mvc = mvc;
        this.mail = mail;
        this.adminPromotionTask = adminPromotionTask;
    }

    public record Session(UUID userId, String email, String firstName, Cookie cookie) {
    }

    public static String uniqueEmail(String prefix) {
        return prefix + "." + SEQUENCE.incrementAndGet() + "." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    public void register(String email, String firstName, String lastName) throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "email", email,
                                "password", PASSWORD,
                                "firstName", firstName,
                                "lastName", lastName,
                                "phoneNumber", "600 100 200",
                                "privacyNoticeAccepted", true))))
                .andExpect(status().isAccepted());
    }

    public String awaitToken(String email, String subjectFragment) {
        return await().atMost(Duration.ofSeconds(10)).until(() -> {
            try {
                return mail.lastTokenFor(email, subjectFragment);
            } catch (AssertionError notYet) {
                return null;
            }
        }, token -> token != null);
    }

    public void verify(String email) throws Exception {
        String token = awaitToken(email, "Potwierdź");
        mvc.perform(MockMvcRequestBuilders.post("/api/auth/verify-email").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("token", token))))
                .andExpect(status().isNoContent());
    }

    public ResultActions attemptLogin(String email, String password) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post("/api/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("email", email, "password", password))));
    }

    public Session login(String email) throws Exception {
        MvcResult result = attemptLogin(email, PASSWORD).andExpect(status().isOk()).andReturn();
        Cookie cookie = result.getResponse().getCookie("KSM_SESSION");
        if (cookie == null) {
            throw new AssertionError("Login did not issue a session cookie");
        }
        String body = result.getResponse().getContentAsString();
        UUID id = UUID.fromString(JsonPath.read(body, "$.user.id"));
        String firstName = JsonPath.read(body, "$.user.firstName");
        return new Session(id, email, firstName, new Cookie(cookie.getName(), cookie.getValue()));
    }

    public Session participant(String firstName) throws Exception {
        String email = uniqueEmail(firstName.toLowerCase());
        register(email, firstName, "Testowa");
        verify(email);
        return login(email);
    }

    public Session admin() throws Exception {
        String email = uniqueEmail("admin");
        register(email, "Admin", "Organizator");
        verify(email);
        adminPromotionTask.promote(email);
        return login(email);
    }

    public ResultActions get(Session session, String url) throws Exception {
        return mvc.perform(authenticated(MockMvcRequestBuilders.get(url), session));
    }

    public ResultActions post(Session session, String url, Object body) throws Exception {
        return mvc.perform(withBody(authenticated(MockMvcRequestBuilders.post(url), session).with(csrf()), body));
    }

    public ResultActions put(Session session, String url, Object body) throws Exception {
        return mvc.perform(withBody(authenticated(MockMvcRequestBuilders.put(url), session).with(csrf()), body));
    }

    public ResultActions delete(Session session, String url) throws Exception {
        return mvc.perform(authenticated(MockMvcRequestBuilders.delete(url), session).with(csrf()));
    }

    public ResultActions mvcDelete(Session session, String url, Object body) throws Exception {
        return mvc.perform(withBody(authenticated(MockMvcRequestBuilders.delete(url), session).with(csrf()), body));
    }

    public UUID createEvent(Session admin, Map<String, Object> overrides) throws Exception {
        Map<String, Object> body = new HashMap<>(Map.of(
                "name", "Wigilia KSM " + SEQUENCE.incrementAndGet(),
                "description", "Spotkanie wigilijne",
                "location", "Sala parafialna",
                "timezone", "Europe/Warsaw",
                "drawMode", "MANUAL"));
        body.putAll(overrides);
        String response = post(admin, "/api/admin/events", body)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(response, "$.id"));
    }

    public String openEventWithInvitation(Session admin, UUID eventId) throws Exception {
        post(admin, "/api/admin/events/" + eventId + "/open-registration", null).andExpect(status().isOk());
        return invitationCode(admin, eventId);
    }

    public String invitationCode(Session admin, UUID eventId) throws Exception {
        String response = post(admin, "/api/admin/events/" + eventId + "/invitation", null)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.code");
    }

    public void join(Session participant, String code) throws Exception {
        post(participant, "/api/invitations/accept", Map.of("code", code)).andExpect(status().isOk());
    }

    public void closeRegistration(Session admin, UUID eventId) throws Exception {
        post(admin, "/api/admin/events/" + eventId + "/close-registration", null).andExpect(status().isOk());
    }

    public ResultActions draw(Session admin, UUID eventId) throws Exception {
        return post(admin, "/api/admin/events/" + eventId + "/draw", Map.of("confirmed", true));
    }

    public record EventScenario(Session admin, UUID eventId, String code, List<Session> participants) {
    }

    public EventScenario openEventWithParticipants(int count) throws Exception {
        Session admin = admin();
        UUID eventId = createEvent(admin, Map.of());
        String code = openEventWithInvitation(admin, eventId);
        List<Session> participants = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Session participant = participant("Uczestnik" + i);
            join(participant, code);
            participants.add(participant);
        }
        return new EventScenario(admin, eventId, code, participants);
    }

    public EventScenario closedEventWithParticipants(int count) throws Exception {
        EventScenario scenario = openEventWithParticipants(count);
        closeRegistration(scenario.admin(), scenario.eventId());
        return scenario;
    }

    public String toJson(Object value) {
        return json.writeValueAsString(value);
    }

    private static MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder builder, Session session) {
        return session == null ? builder : builder.cookie(session.cookie());
    }

    private MockHttpServletRequestBuilder withBody(MockHttpServletRequestBuilder builder, Object body) {
        if (body == null) {
            return builder;
        }
        return builder.contentType(MediaType.APPLICATION_JSON).content(body instanceof String text ? text : toJson(body));
    }
}
