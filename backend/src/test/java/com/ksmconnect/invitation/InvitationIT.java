package com.ksmconnect.invitation;

import com.jayway.jsonpath.JsonPath;
import com.ksmconnect.support.IntegrationTest;
import com.ksmconnect.support.TestApi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class InvitationIT {

    @Autowired
    TestApi api;
    @Autowired
    JdbcClient jdbc;

    @Test
    void generatedInvitationIsShownOnceAndStoredOnlyAsHash() throws Exception {
        TestApi.Session admin = api.admin();
        UUID eventId = api.createEvent(admin, Map.of());
        String body = api.post(admin, "/api/admin/events/" + eventId + "/invitation", null)
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse().getContentAsString();
        String code = JsonPath.read(body, "$.code");
        String joinUrl = JsonPath.read(body, "$.joinUrl");

        assertThat(code).matches("KSM-[A-Z2-9]{5}-[A-Z2-9]{5}");
        assertThat(joinUrl).isEqualTo("https://app.test/join#" + code);
        List<Object> stored = jdbc.sql("SELECT token_hash FROM invitation_tokens WHERE event_id = :id")
                .param("id", eventId).query().singleColumn();
        assertThat(stored).hasSize(1);
        assertThat(stored.getFirst().toString()).doesNotContain(code.replace("-", "").substring(3)).hasSize(64);

        api.get(admin, "/api/admin/events/" + eventId)
                .andExpect(jsonPath("$.invitation.exists").value(true))
                .andExpect(jsonPath("$.invitation.code").doesNotExist());
    }

    @Test
    void participantJoinsWithCodeInAnyFormatting() throws Exception {
        TestApi.Session admin = api.admin();
        UUID eventId = api.createEvent(admin, Map.of());
        String code = api.openEventWithInvitation(admin, eventId);
        TestApi.Session participant = api.participant("Marta");

        api.post(participant, "/api/invitations/preview", Map.of("code", code.toLowerCase().replace("-", " ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId.toString()))
                .andExpect(jsonPath("$.participation").doesNotExist());

        api.post(participant, "/api/invitations/accept", Map.of("code", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newlyJoined").value(true))
                .andExpect(jsonPath("$.event.participation.status").value("ACTIVE"));

        api.get(participant, "/api/events")
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(eventId.toString()));
    }

    @Test
    void joiningTwiceIsIdempotent() throws Exception {
        TestApi.EventScenario scenario = api.openEventWithParticipants(1);
        TestApi.Session participant = scenario.participants().getFirst();
        api.post(participant, "/api/invitations/accept", Map.of("code", scenario.code()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newlyJoined").value(false));
        assertThat(jdbc.sql("SELECT count(*) FROM event_participants WHERE event_id = :id")
                .param("id", scenario.eventId()).query(Long.class).single()).isEqualTo(1);
    }

    @Test
    void concurrentJoinRequestsCreateSingleParticipation() throws Exception {
        TestApi.Session admin = api.admin();
        UUID eventId = api.createEvent(admin, Map.of());
        String code = api.openEventWithInvitation(admin, eventId);
        TestApi.Session participant = api.participant("Rownolegly");
        CountDownLatch start = new CountDownLatch(1);
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            tasks.add(() -> {
                start.await();
                return api.post(participant, "/api/invitations/accept", Map.of("code", code))
                        .andReturn().getResponse().getStatus();
            });
        }
        List<Integer> statuses = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(6)) {
            List<Future<Integer>> futures = tasks.stream().map(executor::submit).toList();
            start.countDown();
            for (Future<Integer> future : futures) {
                statuses.add(future.get());
            }
        }
        assertThat(statuses).allMatch(code1 -> code1 == 200 || code1 == 409);
        assertThat(jdbc.sql("SELECT count(*) FROM event_participants WHERE event_id = :id")
                .param("id", eventId).query(Long.class).single()).isEqualTo(1);
    }

    @Test
    void databasePreventsDuplicateParticipation() throws Exception {
        TestApi.EventScenario scenario = api.openEventWithParticipants(1);
        TestApi.Session participant = scenario.participants().getFirst();
        assertThatThrownBy(() -> jdbc.sql("""
                        INSERT INTO event_participants (id, event_id, user_id, status, joined_at, created_at, updated_at)
                        VALUES (:id, :event, :user, 'ACTIVE', :now, :now, :now)
                        """)
                .param("id", UUID.randomUUID())
                .param("event", scenario.eventId())
                .param("user", participant.userId())
                .param("now", Timestamp.from(Instant.now()))
                .update()).isInstanceOf(DataAccessException.class);
    }

    @Test
    void regeneratingInvitationRevokesPreviousCode() throws Exception {
        TestApi.Session admin = api.admin();
        UUID eventId = api.createEvent(admin, Map.of());
        String oldCode = api.openEventWithInvitation(admin, eventId);
        String newCode = api.invitationCode(admin, eventId);
        TestApi.Session participant = api.participant("Nowy");

        api.post(participant, "/api/invitations/accept", Map.of("code", oldCode))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVALID_INVITATION"));
        api.join(participant, newCode);
        assertThat(jdbc.sql("SELECT count(*) FROM invitation_tokens WHERE event_id = :id AND revoked_at IS NULL")
                .param("id", eventId).query(Long.class).single()).isEqualTo(1);
    }

    @Test
    void invalidCodesAreRejectedUniformly() throws Exception {
        TestApi.Session participant = api.participant("Zgadywacz");
        for (String code : List.of("KSM-AAAAA-AAAAA", "zly kod", "' OR '1'='1", "<script>")) {
            api.post(participant, "/api/invitations/accept", Map.of("code", code))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("INVALID_INVITATION"));
        }
    }

    @Test
    void cannotJoinDraftClosedOrFullEvents() throws Exception {
        TestApi.Session admin = api.admin();
        UUID draft = api.createEvent(admin, Map.of());
        String draftCode = api.invitationCode(admin, draft);
        TestApi.Session participant = api.participant("Spozniony");
        api.post(participant, "/api/invitations/accept", Map.of("code", draftCode))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REGISTRATION_NOT_OPEN"));

        TestApi.EventScenario closed = api.closedEventWithParticipants(1);
        api.post(participant, "/api/invitations/accept", Map.of("code", closed.code()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REGISTRATION_NOT_OPEN"));

        UUID limited = api.createEvent(admin, Map.of("participantLimit", 2));
        String limitedCode = api.openEventWithInvitation(admin, limited);
        api.join(api.participant("Pierwszy"), limitedCode);
        api.join(api.participant("Drugi"), limitedCode);
        api.post(participant, "/api/invitations/accept", Map.of("code", limitedCode))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PARTICIPANT_LIMIT_REACHED"));
    }

    @Test
    void expiredRegistrationWindowRejectsJoin() throws Exception {
        TestApi.Session admin = api.admin();
        UUID eventId = api.createEvent(admin, Map.of(
                "registrationOpensAt", Instant.now().minusSeconds(7200).toString(),
                "registrationClosesAt", Instant.now().minusSeconds(60).toString()));
        String code = api.openEventWithInvitation(admin, eventId);
        api.post(api.participant("Spoznialski"), "/api/invitations/accept", Map.of("code", code))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVALID_INVITATION"));
    }

    @Test
    void participantCanWithdrawAndRejoinWhileRegistrationIsOpen() throws Exception {
        TestApi.EventScenario scenario = api.openEventWithParticipants(1);
        TestApi.Session participant = scenario.participants().getFirst();
        api.post(participant, "/api/events/" + scenario.eventId() + "/withdraw", null).andExpect(status().isNoContent());
        api.get(participant, "/api/events/" + scenario.eventId()).andExpect(status().isNotFound());
        api.join(participant, scenario.code());
        api.get(participant, "/api/events/" + scenario.eventId())
                .andExpect(jsonPath("$.participation.status").value("ACTIVE"));
    }
}
