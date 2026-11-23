package com.ksmconnect.notification;

import com.jayway.jsonpath.JsonPath;
import com.ksmconnect.support.IntegrationTest;
import com.ksmconnect.support.RecordingEmailSender;
import com.ksmconnect.support.RecordingSmsSender;
import com.ksmconnect.support.TestApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class NotificationOutboxIT {

    @Autowired
    TestApi api;
    @Autowired
    OutboxWorker worker;
    @Autowired
    RecordingEmailSender mail;
    @Autowired
    RecordingSmsSender sms;
    @Autowired
    JdbcClient jdbc;

    @AfterEach
    void resetSender() {
        mail.failNext(0);
    }

    @Test
    void sendsPersonalAssignmentEmailToEveryParticipant() throws Exception {
        TestApi.EventScenario scenario = drawn(3);
        drainOutbox();

        for (TestApi.Session participant : scenario.participants()) {
            List<EmailMessage> messages = mail.sentTo(participant.email()).stream()
                    .filter(message -> message.subject().equals(AssignmentMessageComposer.EMAIL_SUBJECT))
                    .toList();
            assertThat(messages).hasSize(1);
            EmailMessage message = messages.getFirst();
            String recipientName = recipientFullName(scenario.eventId(), participant.userId());
            assertThat(message.textBody())
                    .contains("Cześć " + participant.firstName() + "!")
                    .contains("🎁 " + recipientName)
                    .contains("https://app.test/events/" + scenario.eventId() + "/assignment");
            assertThat(message.htmlBody()).contains(recipientName).contains("ZOBACZ SWOJĄ OSOBĘ I LISTĘ PREZENTÓW");
            scenario.participants().stream()
                    .filter(other -> !other.userId().equals(participant.userId()))
                    .map(other -> recipientFullName(scenario.eventId(), other.userId()))
                    .filter(otherRecipient -> !otherRecipient.equals(recipientName))
                    .forEach(otherRecipient -> assertThat(message.textBody()).doesNotContain(otherRecipient));
        }
        assertThat(statuses(scenario.eventId())).containsOnly("SENT");
        assertThat(sms.all()).noneMatch(message -> message.text().contains(scenario.eventId().toString()));
        assertThat(count("SELECT count(*) FROM notification_outbox WHERE event_id = :id AND channel = 'SMS'", scenario.eventId()))
                .isZero();
    }

    @Test
    void failedDeliveryRetriesWithBackoffThenFailsWithoutTouchingDraw() throws Exception {
        TestApi.EventScenario scenario = drawn(2);
        List<Map<String, Object>> assignmentsBefore = assignments(scenario.eventId());
        mail.failNext(1_000);

        worker.processDueNotifications();
        assertThat(statuses(scenario.eventId())).containsOnly("RETRY_PENDING");
        assertThat(count("SELECT count(*) FROM notification_outbox WHERE event_id = :id AND next_attempt_at > now()", scenario.eventId()))
                .isEqualTo(2);

        for (int attempt = 2; attempt <= 3; attempt++) {
            makeDue(scenario.eventId());
            worker.processDueNotifications();
        }
        assertThat(statuses(scenario.eventId())).containsOnly("FAILED");
        assertThat(lastErrors(scenario.eventId())).containsOnly("EMAIL_SEND_FAILED");
        makeDue(scenario.eventId());
        worker.processDueNotifications();
        assertThat(statuses(scenario.eventId())).containsOnly("FAILED");

        assertThat(assignments(scenario.eventId())).isEqualTo(assignmentsBefore);
        assertThat(eventStatus(scenario.eventId())).isEqualTo("DRAW_COMPLETED");

        mail.failNext(0);
        api.post(scenario.admin(), "/api/admin/events/" + scenario.eventId() + "/notifications/retry-failed", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requeued").value(2));
        drainOutbox();
        assertThat(statuses(scenario.eventId())).containsOnly("SENT");
        assertThat(assignments(scenario.eventId())).isEqualTo(assignmentsBefore);
    }

    @Test
    void adminSeesDeliveryStatusAndCanRetrySingleNotification() throws Exception {
        TestApi.EventScenario scenario = drawn(2);
        mail.failNext(1_000);
        for (int attempt = 1; attempt <= 3; attempt++) {
            makeDue(scenario.eventId());
            worker.processDueNotifications();
        }
        mail.failNext(0);
        String body = api.get(scenario.admin(), "/api/admin/events/" + scenario.eventId() + "/notifications")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("FAILED"))
                .andExpect(jsonPath("$[0].attemptCount").value(3))
                .andExpect(jsonPath("$[0].participantName").exists())
                .andReturn().getResponse().getContentAsString();
        String notificationId = JsonPath.read(body, "$[0].id");

        api.post(scenario.admin(), "/api/admin/events/" + scenario.eventId() + "/notifications/" + notificationId + "/retry", null)
                .andExpect(status().isNoContent());
        drainOutbox();
        api.post(scenario.admin(), "/api/admin/events/" + scenario.eventId() + "/notifications/" + notificationId + "/retry", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_RETRYABLE"));
        api.post(scenario.admin(), "/api/admin/events/" + UUID.randomUUID() + "/notifications/" + notificationId + "/retry", null)
                .andExpect(status().isNotFound());
    }

    @Test
    void staleProcessingEntriesAreReclaimedAfterWorkerCrash() throws Exception {
        TestApi.EventScenario scenario = drawn(2);
        jdbc.sql("UPDATE notification_outbox SET status = 'PROCESSING', locked_until = now() - interval '1 minute', attempt_count = 1 WHERE event_id = :id")
                .param("id", scenario.eventId()).update();
        drainOutbox();
        assertThat(statuses(scenario.eventId())).containsOnly("SENT");
    }

    @Test
    void lockedProcessingEntriesAreNotClaimedTwice() throws Exception {
        TestApi.EventScenario scenario = drawn(2);
        jdbc.sql("UPDATE notification_outbox SET status = 'PROCESSING', locked_until = now() + interval '5 minutes' WHERE event_id = :id")
                .param("id", scenario.eventId()).update();
        drainOutbox();
        assertThat(statuses(scenario.eventId())).containsOnly("PROCESSING");
    }

    @Test
    void concurrentWorkersDeliverEachNotificationExactlyOnce() throws Exception {
        TestApi.EventScenario scenario = drawn(8);
        try (ExecutorService executor = Executors.newFixedThreadPool(4)) {
            CompletableFuture<?>[] runs = new CompletableFuture<?>[4];
            for (int i = 0; i < runs.length; i++) {
                runs[i] = CompletableFuture.runAsync(this::drainOutbox, executor);
            }
            CompletableFuture.allOf(runs).join();
        }
        for (TestApi.Session participant : scenario.participants()) {
            assertThat(mail.sentTo(participant.email()).stream()
                    .filter(message -> message.subject().equals(AssignmentMessageComposer.EMAIL_SUBJECT))).hasSize(1);
        }
        assertThat(statuses(scenario.eventId())).containsOnly("SENT");
    }

    @Test
    void databaseEnforcesNotificationIdempotencyKey() throws Exception {
        TestApi.EventScenario scenario = drawn(2);
        assertThatThrownBy(() -> jdbc.sql("""
                        INSERT INTO notification_outbox (id, event_id, participant_id, channel, type, status, idempotency_key,
                            attempt_count, max_attempts, next_attempt_at, created_at, updated_at)
                        SELECT gen_random_uuid(), event_id, participant_id, channel, type, 'PENDING', idempotency_key,
                            0, 3, now(), now(), now()
                        FROM notification_outbox WHERE event_id = :id LIMIT 1
                        """).param("id", scenario.eventId()).update())
                .isInstanceOf(DataAccessException.class);
    }

    private TestApi.EventScenario drawn(int participants) throws Exception {
        TestApi.EventScenario scenario = api.closedEventWithParticipants(participants);
        api.draw(scenario.admin(), scenario.eventId()).andExpect(status().isOk());
        return scenario;
    }

    private void drainOutbox() {
        for (int i = 0; i < 20 && worker.processDueNotifications() > 0; i++) {
        }
    }

    private void makeDue(UUID eventId) {
        jdbc.sql("UPDATE notification_outbox SET next_attempt_at = now() - interval '1 second' WHERE event_id = :id AND status = 'RETRY_PENDING'")
                .param("id", eventId).update();
    }

    private List<String> statuses(UUID eventId) {
        return jdbc.sql("SELECT status FROM notification_outbox WHERE event_id = :id").param("id", eventId)
                .query(String.class).list();
    }

    private List<String> lastErrors(UUID eventId) {
        return jdbc.sql("SELECT last_error FROM notification_outbox WHERE event_id = :id").param("id", eventId)
                .query(String.class).list();
    }

    private List<Map<String, Object>> assignments(UUID eventId) {
        return jdbc.sql("SELECT id, giver_participant_id, recipient_participant_id FROM assignments WHERE event_id = :id ORDER BY id")
                .param("id", eventId).query().listOfRows();
    }

    private String eventStatus(UUID eventId) {
        return jdbc.sql("SELECT status FROM events WHERE id = :id").param("id", eventId).query(String.class).single();
    }

    private long count(String sql, UUID eventId) {
        return jdbc.sql(sql).param("id", eventId).query(Long.class).single();
    }

    private String recipientFullName(UUID eventId, UUID giverUserId) {
        return jdbc.sql("""
                        SELECT ru.first_name || ' ' || ru.last_name FROM assignments a
                        JOIN event_participants g ON g.id = a.giver_participant_id
                        JOIN event_participants r ON r.id = a.recipient_participant_id
                        JOIN users ru ON ru.id = r.user_id
                        WHERE a.event_id = :event AND g.user_id = :user
                        """)
                .param("event", eventId).param("user", giverUserId).query(String.class).single();
    }
}
