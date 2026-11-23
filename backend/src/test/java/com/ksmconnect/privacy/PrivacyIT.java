package com.ksmconnect.privacy;

import com.ksmconnect.support.IntegrationTest;
import com.ksmconnect.support.TestApi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class PrivacyIT {

    @Autowired
    TestApi api;
    @Autowired
    PersonalDataRetentionService retentionService;
    @Autowired
    JdbcClient jdbc;

    @Test
    void completedEventDataIsPurgedAfterRetentionPeriod() throws Exception {
        TestApi.EventScenario scenario = api.closedEventWithParticipants(3);
        api.put(scenario.participants().getFirst(), "/api/events/" + scenario.eventId() + "/wishlist/1",
                Map.of("type", "DESCRIPTION", "description", "Szalik unikalny 91")).andExpect(status().isOk());
        api.draw(scenario.admin(), scenario.eventId()).andExpect(status().isOk());

        assertThat(retentionService.eventsDueForPurge()).doesNotContain(scenario.eventId());
        jdbc.sql("UPDATE events SET starts_at = now() - interval '91 days' WHERE id = :id")
                .param("id", scenario.eventId()).update();
        assertThat(retentionService.eventsDueForPurge()).contains(scenario.eventId());

        retentionService.purgeEvent(scenario.eventId());

        for (String table : new String[]{"assignments", "event_participants", "notification_outbox", "invitation_tokens"}) {
            assertThat(count("SELECT count(*) FROM " + table + " WHERE event_id = :id", scenario.eventId())).as(table).isZero();
        }
        assertThat(jdbc.sql("SELECT count(*) FROM gift_suggestions WHERE description = 'Szalik unikalny 91'")
                .query(Long.class).single()).isZero();
        assertThat(retentionService.eventsDueForPurge()).doesNotContain(scenario.eventId());
        api.get(scenario.participants().getFirst(), "/api/events/" + scenario.eventId() + "/assignment")
                .andExpect(status().isNotFound());
    }

    @Test
    void accountDeletionIsBlockedWhileInCompletedDrawAndAllowedOtherwise() throws Exception {
        TestApi.EventScenario drawn = api.closedEventWithParticipants(2);
        api.draw(drawn.admin(), drawn.eventId()).andExpect(status().isOk());
        TestApi.Session locked = drawn.participants().getFirst();
        api.mvcDelete(locked, "/api/me", Map.of("password", TestApi.PASSWORD))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("ACCOUNT_IN_COMPLETED_DRAW"));

        TestApi.EventScenario open = api.openEventWithParticipants(1);
        TestApi.Session free = open.participants().getFirst();
        api.mvcDelete(free, "/api/me", Map.of("password", "zle haslo do konta"))
                .andExpect(status().isUnauthorized());
        api.mvcDelete(free, "/api/me", Map.of("password", TestApi.PASSWORD)).andExpect(status().isNoContent());
        assertThat(count("SELECT count(*) FROM users WHERE id = :id", free.userId())).isZero();
        assertThat(count("SELECT count(*) FROM event_participants WHERE user_id = :id", free.userId())).isZero();
        api.attemptLogin(free.email(), TestApi.PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void unverifiedAccountsArePurgedAfterThirtyDays() throws Exception {
        String email = TestApi.uniqueEmail("stale");
        api.register(email, "Stary", "Niezweryfikowany");
        api.awaitToken(email, "Potwierdź");
        jdbc.sql("UPDATE users SET created_at = now() - interval '31 days' WHERE email = :email").param("email", email).update();
        retentionService.purgeStaleAccountsAndTokens();
        assertThat(jdbc.sql("SELECT count(*) FROM users WHERE email = :email").param("email", email)
                .query(Long.class).single()).isZero();
    }

    private long count(String sql, UUID id) {
        JdbcClient.StatementSpec statement = jdbc.sql(sql);
        if (id != null) {
            statement = statement.param("id", id);
        }
        return statement.query(Long.class).single();
    }
}
