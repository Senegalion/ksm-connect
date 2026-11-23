package com.ksmconnect.scheduler;

import com.ksmconnect.support.IntegrationTest;
import com.ksmconnect.support.TestApi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@IntegrationTest
class SchedulerIT {

    @Autowired
    TestApi api;
    @Autowired
    EventLifecycleScheduler scheduler;
    @Autowired
    JdbcClient jdbc;

    @Test
    void scheduledDrawClosesRegistrationAndRunsOnceWhenDue() throws Exception {
        TestApi.Session admin = api.admin();
        Instant drawAt = Instant.now().plusSeconds(3600);
        UUID eventId = api.createEvent(admin, Map.of("drawMode", "SCHEDULED", "drawAt", drawAt.toString()));
        String code = api.openEventWithInvitation(admin, eventId);
        for (int i = 0; i < 3; i++) {
            api.join(api.participant("Zaplanowany" + i), code);
        }

        scheduler.tick();
        assertThat(status(eventId)).isEqualTo("REGISTRATION_OPEN");

        jdbc.sql("UPDATE events SET draw_at = now() - interval '1 second' WHERE id = :id").param("id", eventId).update();
        scheduler.tick();
        assertThat(status(eventId)).isEqualTo("DRAW_COMPLETED");
        assertThat(assignmentCount(eventId)).isEqualTo(3);

        scheduler.tick();
        assertThat(assignmentCount(eventId)).isEqualTo(3);
        assertThat(jdbc.sql("SELECT count(*) FROM audit_events WHERE event_id = :id AND type = 'DRAW_COMPLETED'")
                .param("id", eventId).query(Long.class).single()).isEqualTo(1);
    }

    @Test
    void manualEventsAreOnlyClosedNeverDrawnAutomatically() throws Exception {
        TestApi.Session admin = api.admin();
        UUID eventId = api.createEvent(admin, Map.of(
                "registrationClosesAt", Instant.now().plusSeconds(3600).toString(),
                "drawAt", Instant.now().plusSeconds(7200).toString()));
        String code = api.openEventWithInvitation(admin, eventId);
        api.join(api.participant("Reczny1"), code);
        api.join(api.participant("Reczny2"), code);
        jdbc.sql("UPDATE events SET registration_closes_at = now() - interval '2 seconds', draw_at = now() - interval '1 second' WHERE id = :id")
                .param("id", eventId).update();

        scheduler.tick();
        assertThat(status(eventId)).isEqualTo("REGISTRATION_CLOSED");
        assertThat(assignmentCount(eventId)).isZero();
    }

    @Test
    void scheduledDrawWithTooFewParticipantsStaysPendingAndIsFlaggedOverdue() throws Exception {
        TestApi.Session admin = api.admin();
        UUID eventId = api.createEvent(admin, Map.of("drawMode", "SCHEDULED",
                "drawAt", Instant.now().plusSeconds(3600).toString()));
        String code = api.openEventWithInvitation(admin, eventId);
        api.join(api.participant("Samotny"), code);
        jdbc.sql("UPDATE events SET draw_at = now() - interval '1 second' WHERE id = :id").param("id", eventId).update();

        scheduler.tick();
        assertThat(status(eventId)).isEqualTo("REGISTRATION_CLOSED");
        assertThat(assignmentCount(eventId)).isZero();
        api.get(admin, "/api/admin/events/" + eventId).andExpect(jsonPath("$.drawOverdue").value(true));
    }

    private String status(UUID eventId) {
        return jdbc.sql("SELECT status FROM events WHERE id = :id").param("id", eventId).query(String.class).single();
    }

    private long assignmentCount(UUID eventId) {
        return jdbc.sql("SELECT count(*) FROM assignments WHERE event_id = :id").param("id", eventId).query(Long.class).single();
    }
}
