package com.ksmconnect.draw;

import com.jayway.jsonpath.JsonPath;
import com.ksmconnect.common.audit.AuditLog;
import com.ksmconnect.common.audit.AuditType;
import com.ksmconnect.support.IntegrationTest;
import com.ksmconnect.support.TestApi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class DrawIT {

    @Autowired
    TestApi api;
    @Autowired
    JdbcClient jdbc;
    @Autowired
    DrawService drawService;
    @Autowired
    TransactionTemplate transactionTemplate;
    @MockitoSpyBean
    AuditLog auditLog;

    @Test
    void drawForThirtyParticipantsSatisfiesAllInvariantsInDatabase() throws Exception {
        TestApi.EventScenario scenario = api.closedEventWithParticipants(30);

        api.draw(scenario.admin(), scenario.eventId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantCount").value(30));

        List<Map<String, Object>> rows = assignmentRows(scenario.eventId());
        Set<Object> participants = new HashSet<>(jdbc.sql(
                        "SELECT id FROM event_participants WHERE event_id = :id AND status = 'ACTIVE'")
                .param("id", scenario.eventId()).query().singleColumn());
        Set<Object> givers = new HashSet<>();
        Set<Object> recipients = new HashSet<>();
        for (Map<String, Object> row : rows) {
            assertThat(row.get("giver_participant_id")).isNotEqualTo(row.get("recipient_participant_id"));
            givers.add(row.get("giver_participant_id"));
            recipients.add(row.get("recipient_participant_id"));
        }
        assertThat(rows).hasSize(30);
        assertThat(givers).isEqualTo(participants);
        assertThat(recipients).isEqualTo(participants);
        assertThat(eventStatus(scenario.eventId())).isEqualTo("DRAW_COMPLETED");
        assertThat(count("SELECT count(*) FROM notification_outbox WHERE event_id = :id AND channel = 'EMAIL'", scenario.eventId()))
                .isEqualTo(30);
    }

    @Test
    void everyParticipantSeesExactlyTheirOwnRecipient() throws Exception {
        TestApi.EventScenario scenario = api.closedEventWithParticipants(5);
        api.draw(scenario.admin(), scenario.eventId()).andExpect(status().isOk());

        Map<UUID, String> nameByUser = new HashMap<>();
        for (TestApi.Session participant : scenario.participants()) {
            nameByUser.put(participant.userId(), participant.firstName());
        }
        Set<String> seenRecipients = new HashSet<>();
        for (TestApi.Session participant : scenario.participants()) {
            String body = api.get(participant, "/api/events/" + scenario.eventId() + "/assignment")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.drawCompleted").value(true))
                    .andReturn().getResponse().getContentAsString();
            String recipientName = JsonPath.read(body, "$.recipient.firstName");
            assertThat(recipientName).isNotEqualTo(participant.firstName());
            assertThat(seenRecipients.add(recipientName)).isTrue();
            assertThat(expectedRecipientFor(scenario.eventId(), participant.userId()))
                    .isEqualTo(recipientName);
        }
        assertThat(seenRecipients).containsExactlyInAnyOrderElementsOf(nameByUser.values());
    }

    @Test
    void revealIsRememberedPerParticipantOnlyAfterDraw() throws Exception {
        TestApi.EventScenario scenario = api.closedEventWithParticipants(3);
        TestApi.Session first = scenario.participants().get(0);
        TestApi.Session second = scenario.participants().get(1);
        String base = "/api/events/" + scenario.eventId() + "/assignment";

        api.post(first, base + "/reveal", null).andExpect(status().isOk()).andExpect(jsonPath("$.revealed").value(false));
        api.draw(scenario.admin(), scenario.eventId()).andExpect(status().isOk());

        api.get(first, base).andExpect(jsonPath("$.revealed").value(false));
        api.post(first, base + "/reveal", null).andExpect(status().isOk()).andExpect(jsonPath("$.revealed").value(true));
        api.post(first, base + "/reveal", null).andExpect(jsonPath("$.revealed").value(true));
        api.get(first, base).andExpect(jsonPath("$.revealed").value(true)).andExpect(jsonPath("$.recipient.firstName").exists());
        api.get(second, base).andExpect(jsonPath("$.revealed").value(false));
        api.post(api.participant("Obcy"), base + "/reveal", null).andExpect(status().isNotFound());
        assertThat(assignmentRows(scenario.eventId())).hasSize(3);
    }

    @Test
    void secondDrawIsRejectedAndAssignmentsStayUnchanged() throws Exception {
        TestApi.EventScenario scenario = api.closedEventWithParticipants(4);
        api.draw(scenario.admin(), scenario.eventId()).andExpect(status().isOk());
        List<Map<String, Object>> before = assignmentRows(scenario.eventId());

        api.draw(scenario.admin(), scenario.eventId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DRAW_ALREADY_COMPLETED"));
        assertThat(drawService.drawIfScheduledAndDue(scenario.eventId())).isEmpty();
        assertThat(assignmentRows(scenario.eventId())).isEqualTo(before);
    }

    @Test
    void concurrentDrawRequestsProduceExactlyOneDraw() throws Exception {
        TestApi.EventScenario scenario = api.closedEventWithParticipants(6);
        int threads = 8;
        CountDownLatch start = new CountDownLatch(1);
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(() -> {
                start.await();
                return api.draw(scenario.admin(), scenario.eventId()).andReturn().getResponse().getStatus();
            });
        }
        List<Integer> statuses = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            List<Future<Integer>> futures = tasks.stream().map(executor::submit).toList();
            start.countDown();
            for (Future<Integer> future : futures) {
                statuses.add(future.get());
            }
        }
        assertThat(statuses).containsOnlyOnce(200);
        assertThat(statuses.stream().filter(code -> code == 409)).hasSize(threads - 1);
        assertThat(assignmentRows(scenario.eventId())).hasSize(6);
        assertThat(count("SELECT count(*) FROM notification_outbox WHERE event_id = :id", scenario.eventId())).isEqualTo(6);
    }

    @Test
    void drawRequiresClosedRegistration() throws Exception {
        TestApi.EventScenario scenario = api.openEventWithParticipants(3);
        api.draw(scenario.admin(), scenario.eventId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REGISTRATION_NOT_CLOSED"));
        assertThat(assignmentRows(scenario.eventId())).isEmpty();
    }

    @Test
    void drawRequiresAtLeastTwoParticipants() throws Exception {
        TestApi.EventScenario scenario = api.closedEventWithParticipants(1);
        api.draw(scenario.admin(), scenario.eventId())
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("NOT_ENOUGH_PARTICIPANTS"));
        assertThat(eventStatus(scenario.eventId())).isEqualTo("REGISTRATION_CLOSED");
    }

    @Test
    void drawRequiresExplicitConfirmation() throws Exception {
        TestApi.EventScenario scenario = api.closedEventWithParticipants(2);
        api.post(scenario.admin(), "/api/admin/events/" + scenario.eventId() + "/draw", Map.of("confirmed", false))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("DRAW_CONFIRMATION_REQUIRED"));
        assertThat(assignmentRows(scenario.eventId())).isEmpty();
    }

    @Test
    void withdrawnParticipantsAreExcludedFromDraw() throws Exception {
        TestApi.EventScenario scenario = api.openEventWithParticipants(4);
        api.post(scenario.participants().getFirst(), "/api/events/" + scenario.eventId() + "/withdraw", null)
                .andExpect(status().isNoContent());
        api.closeRegistration(scenario.admin(), scenario.eventId());
        api.draw(scenario.admin(), scenario.eventId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantCount").value(3));
        api.get(scenario.participants().getFirst(), "/api/events/" + scenario.eventId() + "/assignment")
                .andExpect(status().isNotFound());
    }

    @Test
    void failureLateInDrawTransactionRollsBackEverything() throws Exception {
        TestApi.EventScenario scenario = api.closedEventWithParticipants(4);
        doThrow(new IllegalStateException("simulated failure before commit"))
                .when(auditLog).record(eq(AuditType.DRAW_COMPLETED), any(), any(), any());
        try {
            api.draw(scenario.admin(), scenario.eventId()).andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
        } finally {
            doCallRealMethod().when(auditLog).record(eq(AuditType.DRAW_COMPLETED), any(), any(), any());
        }
        assertThat(assignmentRows(scenario.eventId())).isEmpty();
        assertThat(eventStatus(scenario.eventId())).isEqualTo("REGISTRATION_CLOSED");
        assertThat(count("SELECT count(*) FROM notification_outbox WHERE event_id = :id", scenario.eventId())).isZero();

        api.draw(scenario.admin(), scenario.eventId()).andExpect(status().isOk());
        assertThat(assignmentRows(scenario.eventId())).hasSize(4);
    }

    @Test
    void databaseRejectsModificationOfCompletedAssignments() throws Exception {
        TestApi.EventScenario scenario = api.closedEventWithParticipants(3);
        api.draw(scenario.admin(), scenario.eventId()).andExpect(status().isOk());
        UUID eventId = scenario.eventId();

        assertThatThrownBy(() -> jdbc.sql("UPDATE assignments SET recipient_participant_id = giver_participant_id WHERE event_id = :id")
                .param("id", eventId).update()).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.sql("DELETE FROM assignments WHERE event_id = :id")
                .param("id", eventId).update()).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.sql("UPDATE events SET status = 'REGISTRATION_OPEN' WHERE id = :id")
                .param("id", eventId).update()).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.sql("UPDATE event_participants SET status = 'WITHDRAWN', withdrawn_at = now() WHERE event_id = :id")
                .param("id", eventId).update()).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.sql("DELETE FROM event_participants WHERE event_id = :id")
                .param("id", eventId).update()).isInstanceOf(DataAccessException.class);
        assertThat(assignmentRows(eventId)).hasSize(3);
    }

    @Test
    void databaseRejectsSelfAssignmentDuplicatesAndForeignParticipants() throws Exception {
        TestApi.EventScenario first = api.closedEventWithParticipants(3);
        TestApi.EventScenario other = api.closedEventWithParticipants(2);
        List<Object> ids = participantIds(first.eventId());
        Object foreign = participantIds(other.eventId()).getFirst();

        assertThatThrownBy(() -> insertAssignment(first.eventId(), ids.get(0), ids.get(0)))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> insertAssignment(first.eventId(), ids.get(0), foreign))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            insertAssignment(first.eventId(), ids.get(0), ids.get(1));
            insertAssignment(first.eventId(), ids.get(2), ids.get(1));
        })).isInstanceOf(DataAccessException.class);
        assertThat(assignmentRows(first.eventId())).isEmpty();
    }

    @Test
    void databaseRejectsCompletingDrawWithIncompleteAssignments() throws Exception {
        TestApi.EventScenario scenario = api.closedEventWithParticipants(3);
        List<Object> ids = participantIds(scenario.eventId());
        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            insertAssignment(scenario.eventId(), ids.get(0), ids.get(1));
            insertAssignment(scenario.eventId(), ids.get(1), ids.get(0));
            jdbc.sql("UPDATE events SET status = 'DRAW_COMPLETED', draw_completed_at = now() WHERE id = :id")
                    .param("id", scenario.eventId()).update();
        })).isInstanceOf(DataAccessException.class);
        assertThat(eventStatus(scenario.eventId())).isEqualTo("REGISTRATION_CLOSED");
        assertThat(assignmentRows(scenario.eventId())).isEmpty();
    }

    @Test
    void databaseRejectsAssignmentsWhileRegistrationIsOpen() throws Exception {
        TestApi.EventScenario scenario = api.openEventWithParticipants(2);
        List<Object> ids = participantIds(scenario.eventId());
        assertThatThrownBy(() -> insertAssignment(scenario.eventId(), ids.get(0), ids.get(1)))
                .isInstanceOf(DataAccessException.class);
    }

    private void insertAssignment(UUID eventId, Object giver, Object recipient) {
        jdbc.sql("""
                        INSERT INTO assignments (id, event_id, giver_participant_id, recipient_participant_id, created_at, updated_at)
                        VALUES (:id, :event, :giver, :recipient, :now, :now)
                        """)
                .param("id", UUID.randomUUID())
                .param("event", eventId)
                .param("giver", giver)
                .param("recipient", recipient)
                .param("now", Timestamp.from(Instant.now()))
                .update();
    }

    private String expectedRecipientFor(UUID eventId, UUID giverUserId) {
        return jdbc.sql("""
                        SELECT ru.first_name FROM assignments a
                        JOIN event_participants g ON g.id = a.giver_participant_id
                        JOIN event_participants r ON r.id = a.recipient_participant_id
                        JOIN users ru ON ru.id = r.user_id
                        WHERE a.event_id = :event AND g.user_id = :user
                        """)
                .param("event", eventId).param("user", giverUserId)
                .query(String.class).single();
    }

    private List<Object> participantIds(UUID eventId) {
        return jdbc.sql("SELECT id FROM event_participants WHERE event_id = :id ORDER BY id")
                .param("id", eventId).query().singleColumn();
    }

    private List<Map<String, Object>> assignmentRows(UUID eventId) {
        return jdbc.sql("SELECT giver_participant_id, recipient_participant_id FROM assignments WHERE event_id = :id ORDER BY giver_participant_id")
                .param("id", eventId).query().listOfRows();
    }

    private String eventStatus(UUID eventId) {
        return jdbc.sql("SELECT status FROM events WHERE id = :id").param("id", eventId).query(String.class).single();
    }

    private long count(String sql, UUID eventId) {
        return jdbc.sql(sql).param("id", eventId).query(Long.class).single();
    }
}
