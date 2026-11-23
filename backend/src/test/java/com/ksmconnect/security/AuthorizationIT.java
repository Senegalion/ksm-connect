package com.ksmconnect.security;

import com.ksmconnect.support.IntegrationTest;
import com.ksmconnect.support.TestApi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class AuthorizationIT {

    @Autowired
    TestApi api;
    @Autowired
    MockMvc mvc;

    @Test
    void anonymousUsersCannotAccessProtectedEndpoints() throws Exception {
        UUID eventId = UUID.randomUUID();
        for (String url : List.of("/api/me", "/api/events", "/api/events/" + eventId,
                "/api/events/" + eventId + "/assignment", "/api/events/" + eventId + "/wishlist", "/api/admin/events")) {
            mvc.perform(get(url))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        }
        mvc.perform(post("/api/invitations/accept").with(csrf()).contentType("application/json").content("{\"code\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void participantsCannotUseAdministrativeEndpoints() throws Exception {
        TestApi.EventScenario scenario = api.closedEventWithParticipants(2);
        TestApi.Session participant = scenario.participants().getFirst();
        String base = "/api/admin/events/" + scenario.eventId();

        api.get(participant, "/api/admin/events").andExpect(status().isForbidden());
        api.get(participant, base + "/participants").andExpect(status().isForbidden());
        api.post(participant, base + "/invitation", null).andExpect(status().isForbidden());
        api.draw(participant, scenario.eventId()).andExpect(status().isForbidden());
        api.post(participant, "/api/admin/events", Map.of("name", "x", "timezone", "Europe/Warsaw", "drawMode", "MANUAL"))
                .andExpect(status().isForbidden());
        api.get(participant, base + "/notifications").andExpect(status().isForbidden());
    }

    @Test
    void outsidersCannotSeeOrModifyAnotherEvent() throws Exception {
        TestApi.EventScenario scenario = api.closedEventWithParticipants(3);
        api.put(scenario.participants().get(1), "/api/events/" + scenario.eventId() + "/wishlist/1",
                Map.of("type", "DESCRIPTION", "description", "sekret")).andReturn();
        api.draw(scenario.admin(), scenario.eventId()).andExpect(status().isOk());
        TestApi.Session outsider = api.participant("Obcy");
        String base = "/api/events/" + scenario.eventId();

        api.get(outsider, base).andExpect(status().isNotFound());
        api.get(outsider, base + "/assignment").andExpect(status().isNotFound());
        api.get(outsider, base + "/wishlist").andExpect(status().isNotFound());
        api.put(outsider, base + "/wishlist/1", Map.of("type", "DESCRIPTION", "description", "atak"))
                .andExpect(status().isNotFound());
        api.post(outsider, base + "/withdraw", null).andExpect(status().isNotFound());
        api.get(outsider, "/api/events").andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void adminEndpointsNeverExposeAssignmentsOrWishlists() throws Exception {
        TestApi.EventScenario scenario = api.closedEventWithParticipants(3);
        TestApi.Session participant = scenario.participants().getFirst();
        api.put(participant, "/api/events/" + scenario.eventId() + "/wishlist/1",
                Map.of("type", "DESCRIPTION", "description", "Tajny prezent XYZ")).andExpect(status().isOk());
        api.draw(scenario.admin(), scenario.eventId()).andExpect(status().isOk());
        String base = "/api/admin/events/" + scenario.eventId();

        for (String url : List.of("/api/admin/events", base, base + "/participants", base + "/notifications", base + "/audit")) {
            String body = api.get(scenario.admin(), url).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            assertThat(body).doesNotContainIgnoringCase("recipient")
                    .doesNotContainIgnoringCase("giver")
                    .doesNotContain("Tajny prezent XYZ");
        }
        api.get(scenario.admin(), "/api/admin/events/" + scenario.eventId() + "/assignments").andExpect(status().isNotFound());
    }

    @Test
    void adminWhoJoinsAsParticipantSeesOnlyOwnAssignment() throws Exception {
        TestApi.EventScenario scenario = api.openEventWithParticipants(2);
        api.join(scenario.admin(), scenario.code());
        api.closeRegistration(scenario.admin(), scenario.eventId());
        api.draw(scenario.admin(), scenario.eventId())
                .andExpect(jsonPath("$.participantCount").value(3));

        api.get(scenario.admin(), "/api/events/" + scenario.eventId() + "/assignment")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipient.firstName").exists())
                .andExpect(jsonPath("$.recipient.firstName").value(org.hamcrest.Matchers.not("Admin")));
    }

    @Test
    void participantCannotRemoveOthersOrChangeEventState() throws Exception {
        TestApi.EventScenario scenario = api.openEventWithParticipants(2);
        TestApi.Session participant = scenario.participants().getFirst();
        api.post(participant, "/api/admin/events/" + scenario.eventId() + "/close-registration", null)
                .andExpect(status().isForbidden());
        api.delete(participant, "/api/admin/events/" + scenario.eventId() + "/participants/" + UUID.randomUUID())
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCannotRemoveParticipantFromAnotherEventById() throws Exception {
        TestApi.EventScenario first = api.openEventWithParticipants(1);
        TestApi.EventScenario second = api.openEventWithParticipants(1);
        String body = api.get(second.admin(), "/api/admin/events/" + second.eventId() + "/participants")
                .andReturn().getResponse().getContentAsString();
        String foreignParticipant = com.jayway.jsonpath.JsonPath.read(body, "$[0].participantId");
        api.delete(first.admin(), "/api/admin/events/" + first.eventId() + "/participants/" + foreignParticipant)
                .andExpect(status().isNotFound());
    }
}
