package com.ksmconnect.event;

import com.ksmconnect.support.IntegrationTest;
import com.ksmconnect.support.TestApi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class EventAdminIT {

    @Autowired
    TestApi api;
    @Autowired
    JdbcClient jdbc;

    @Test
    void adminCreatesConfiguresAndListsEvents() throws Exception {
        TestApi.Session admin = api.admin();
        Instant closes = Instant.parse("2026-12-20T20:00:00Z");
        UUID eventId = api.createEvent(admin, Map.of(
                "name", "KSM Wigilia 2026",
                "startsAt", "2026-12-22T17:00:00Z",
                "registrationClosesAt", closes.toString(),
                "drawMode", "SCHEDULED",
                "drawAt", "2026-12-20T21:00:00Z",
                "participantLimit", 40));

        api.get(admin, "/api/admin/events/" + eventId)
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.drawMode").value("SCHEDULED"))
                .andExpect(jsonPath("$.registrationClosesAt").value("2026-12-20T20:00:00Z"))
                .andExpect(jsonPath("$.timezone").value("Europe/Warsaw"));

        Map<String, Object> update = new HashMap<>(Map.of("name", "KSM Wigilia 2026 – Parafia", "timezone", "Europe/Warsaw",
                "drawMode", "MANUAL"));
        api.put(admin, "/api/admin/events/" + eventId, update)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("KSM Wigilia 2026 – Parafia"))
                .andExpect(jsonPath("$.drawMode").value("MANUAL"));

        api.get(admin, "/api/admin/events").andExpect(status().isOk());
    }

    @Test
    void rejectsInvalidEventConfiguration() throws Exception {
        TestApi.Session admin = api.admin();
        api.post(admin, "/api/admin/events", Map.of("name", "X", "timezone", "Europe/Warsaw", "drawMode", "SCHEDULED"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("DRAW_TIME_REQUIRED"));
        api.post(admin, "/api/admin/events", Map.of("name", "X", "timezone", "Nowhere/City", "drawMode", "MANUAL"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_TIMEZONE"));
        api.post(admin, "/api/admin/events", Map.of("name", "", "timezone", "Europe/Warsaw", "drawMode", "MANUAL"))
                .andExpect(status().isBadRequest());
        api.post(admin, "/api/admin/events", Map.of("name", "X", "timezone", "Europe/Warsaw", "drawMode", "MANUAL",
                        "registrationOpensAt", "2026-12-10T10:00:00Z", "registrationClosesAt", "2026-12-01T10:00:00Z"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_REGISTRATION_WINDOW"));
    }

    @Test
    void enforcesLifecycleTransitions() throws Exception {
        TestApi.Session admin = api.admin();
        UUID eventId = api.createEvent(admin, Map.of());
        String base = "/api/admin/events/" + eventId;

        api.post(admin, base + "/close-registration", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_EVENT_TRANSITION"));
        api.post(admin, base + "/open-registration", null).andExpect(jsonPath("$.status").value("REGISTRATION_OPEN"));
        api.post(admin, base + "/close-registration", null).andExpect(jsonPath("$.status").value("REGISTRATION_CLOSED"));
        api.post(admin, base + "/open-registration", null).andExpect(jsonPath("$.status").value("REGISTRATION_OPEN"));
        api.post(admin, base + "/cancel", null).andExpect(jsonPath("$.status").value("CANCELLED"));
        api.post(admin, base + "/open-registration", null).andExpect(status().isConflict());
        api.post(admin, base + "/invitation", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVITATION_NOT_ALLOWED"));
        api.put(admin, base, Map.of("name", "x", "timezone", "Europe/Warsaw", "drawMode", "MANUAL"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EVENT_NOT_CONFIGURABLE"));
    }

    @Test
    void databaseRejectsIllegalStatusTransitions() throws Exception {
        TestApi.Session admin = api.admin();
        UUID eventId = api.createEvent(admin, Map.of());
        assertThatThrownBy(() -> jdbc.sql("UPDATE events SET status = 'REGISTRATION_CLOSED' WHERE id = :id")
                .param("id", eventId).update()).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.sql("UPDATE events SET status = 'DRAW_COMPLETED', draw_completed_at = now() WHERE id = :id")
                .param("id", eventId).update()).isInstanceOf(DataAccessException.class);
    }

    @Test
    void adminCanRemoveParticipantBeforeDrawButNotAfter() throws Exception {
        TestApi.EventScenario scenario = api.closedEventWithParticipants(3);
        String body = api.get(scenario.admin(), "/api/admin/events/" + scenario.eventId() + "/participants")
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].email").exists())
                .andReturn().getResponse().getContentAsString();
        String participantId = com.jayway.jsonpath.JsonPath.read(body, "$[0].participantId");
        api.delete(scenario.admin(), "/api/admin/events/" + scenario.eventId() + "/participants/" + participantId)
                .andExpect(status().isNoContent());
        api.draw(scenario.admin(), scenario.eventId()).andExpect(jsonPath("$.participantCount").value(2));

        String secondId = com.jayway.jsonpath.JsonPath.read(
                api.get(scenario.admin(), "/api/admin/events/" + scenario.eventId() + "/participants")
                        .andReturn().getResponse().getContentAsString(), "$[1].participantId");
        api.delete(scenario.admin(), "/api/admin/events/" + scenario.eventId() + "/participants/" + secondId)
                .andExpect(status().isConflict());
        api.get(scenario.admin(), "/api/admin/events/" + scenario.eventId() + "/audit")
                .andExpect(jsonPath("$[0].type").value("DRAW_COMPLETED"));
    }

    @Test
    void unknownEventsReturnNotFound() throws Exception {
        TestApi.Session admin = api.admin();
        api.get(admin, "/api/admin/events/" + UUID.randomUUID()).andExpect(status().isNotFound());
        api.get(admin, "/api/admin/events/not-a-uuid").andExpect(status().isBadRequest());
    }
}
