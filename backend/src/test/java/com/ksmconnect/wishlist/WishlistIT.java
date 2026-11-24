package com.ksmconnect.wishlist;

import com.ksmconnect.support.IntegrationTest;
import com.ksmconnect.support.TestApi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class WishlistIT {

    @Autowired
    TestApi api;
    @Autowired
    JdbcClient jdbc;

    @Test
    void participantManagesUpToThreeSuggestions() throws Exception {
        TestApi.EventScenario scenario = api.openEventWithParticipants(1);
        TestApi.Session participant = scenario.participants().getFirst();
        String base = "/api/events/" + scenario.eventId() + "/wishlist";

        api.get(participant, base)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.editable").value(true))
                .andExpect(jsonPath("$.filledCount").value(0))
                .andExpect(jsonPath("$.maxItems").value(3));

        api.put(participant, base + "/1", Map.of("type", "URL", "url", "https://example.com/ksiazka"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filledCount").value(1));
        api.put(participant, base + "/3", Map.of("type", "DESCRIPTION", "description", "Dobra gra strategiczna"))
                .andExpect(jsonPath("$.filledCount").value(2))
                .andExpect(jsonPath("$.items[1].position").value(3));
        api.put(participant, base + "/1", Map.of("type", "DESCRIPTION", "description", "Ciepły szalik"))
                .andExpect(jsonPath("$.filledCount").value(2))
                .andExpect(jsonPath("$.items[0].type").value("DESCRIPTION"))
                .andExpect(jsonPath("$.items[0].url").doesNotExist());
        api.delete(participant, base + "/3")
                .andExpect(jsonPath("$.filledCount").value(1));
    }

    @Test
    void acceptsVeryLongShopLinkAndStoresCleanedVersion() throws Exception {
        TestApi.EventScenario scenario = api.openEventWithParticipants(1);
        TestApi.Session participant = scenario.participants().getFirst();
        String base = "/api/events/" + scenario.eventId() + "/wishlist";
        String link = TrackingParametersTest.empikFacebookLink();

        api.put(participant, base + "/1", Map.of("type", "URL", "url", link))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].url").value(
                        "https://www.empik.com/zakreslacze-stabilo-boss-original-pastel-6-sztuk-stabilo,p1242414260,szkolne-i-papiernicze-p"
                                + "?mpShopId=0&destinationUrl=product%2Fp1242414260&media_type=image"));

        String longClean = "https://example.com/" + "x".repeat(6000);
        api.put(participant, base + "/2", Map.of("type", "URL", "url", longClean))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[1].url").value(longClean));
    }

    @Test
    void rejectsInvalidPositionsAndUnsafeContent() throws Exception {
        TestApi.EventScenario scenario = api.openEventWithParticipants(1);
        TestApi.Session participant = scenario.participants().getFirst();
        String base = "/api/events/" + scenario.eventId() + "/wishlist";

        api.put(participant, base + "/4", Map.of("type", "DESCRIPTION", "description", "x"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_SUGGESTION_POSITION"));
        api.put(participant, base + "/0", Map.of("type", "DESCRIPTION", "description", "x"))
                .andExpect(status().isUnprocessableContent());
        api.put(participant, base + "/1", Map.of("type", "URL", "url", "javascript:alert(document.cookie)"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_URL"));
        api.put(participant, base + "/1", Map.of("type", "URL", "url", "https://example.com", "description", "oba"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("SUGGESTION_HAS_BOTH_URL_AND_DESCRIPTION"));
        api.put(participant, base + "/1", Map.of("type", "GIFT_CARD", "description", "x"))
                .andExpect(status().isBadRequest());
        api.put(participant, base + "/1", Map.of("type", "DESCRIPTION", "description", "a".repeat(501)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void databaseConstraintsGuardSuggestions() throws Exception {
        TestApi.EventScenario scenario = api.openEventWithParticipants(1);
        UUID participantId = jdbc.sql("SELECT id FROM event_participants WHERE event_id = :id")
                .param("id", scenario.eventId()).query(UUID.class).single();

        assertThatThrownBy(() -> insertSuggestion(participantId, 1, "URL", "javascript:alert(1)", null))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> insertSuggestion(participantId, 4, "DESCRIPTION", null, "x"))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> insertSuggestion(participantId, 1, "URL", "https://a.pl", "both"))
                .isInstanceOf(DataAccessException.class);
        insertSuggestion(participantId, 2, "DESCRIPTION", null, "ok");
        assertThatThrownBy(() -> insertSuggestion(participantId, 2, "DESCRIPTION", null, "duplicate position"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void wishlistStaysEditableAfterDrawAndGiverSeesUpdates() throws Exception {
        TestApi.EventScenario scenario = api.openEventWithParticipants(2);
        TestApi.Session first = scenario.participants().get(0);
        TestApi.Session second = scenario.participants().get(1);
        String base = "/api/events/" + scenario.eventId();
        api.put(second, base + "/wishlist/1", Map.of("type", "URL", "url", "https://example.com/gra")).andExpect(status().isOk());
        api.put(second, base + "/wishlist/2", Map.of("type", "DESCRIPTION", "description", "<b>kubek</b>")).andExpect(status().isOk());

        api.get(first, base + "/assignment")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.drawCompleted").value(false))
                .andExpect(jsonPath("$.recipient").doesNotExist());

        api.closeRegistration(scenario.admin(), scenario.eventId());
        api.draw(scenario.admin(), scenario.eventId()).andExpect(status().isOk());

        api.get(first, base + "/assignment")
                .andExpect(jsonPath("$.recipient.firstName").value(second.firstName()))
                .andExpect(jsonPath("$.recipient.wishlist.length()").value(2))
                .andExpect(jsonPath("$.recipient.wishlist[0].url").value("https://example.com/gra"))
                .andExpect(jsonPath("$.recipient.wishlist[1].description").value("<b>kubek</b>"));
        api.get(second, base + "/assignment")
                .andExpect(jsonPath("$.recipient.firstName").value(first.firstName()))
                .andExpect(jsonPath("$.recipient.wishlist.length()").value(0));

        api.get(second, base + "/wishlist").andExpect(jsonPath("$.editable").value(true));
        api.put(second, base + "/wishlist/3", Map.of("type", "DESCRIPTION", "description", "Dopisane po losowaniu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filledCount").value(3));
        api.delete(second, base + "/wishlist/2").andExpect(status().isOk());
        api.get(first, base + "/assignment")
                .andExpect(jsonPath("$.recipient.wishlist.length()").value(2))
                .andExpect(jsonPath("$.recipient.wishlist[1].description").value("Dopisane po losowaniu"));
    }

    @Test
    void wishlistIsEditableAfterRegistrationClosesAndLocksOnlyAtConfiguredDeadline() throws Exception {
        TestApi.EventScenario scenario = api.closedEventWithParticipants(2);
        String base = "/api/events/" + scenario.eventId() + "/wishlist";
        TestApi.Session participant = scenario.participants().getFirst();
        api.put(participant, base + "/1", Map.of("type", "DESCRIPTION", "description", "Po zamknięciu zapisów"))
                .andExpect(status().isOk());

        jdbc.sql("UPDATE events SET wishlist_deadline = now() - interval '1 minute' WHERE id = :id")
                .param("id", scenario.eventId()).update();
        api.get(participant, base).andExpect(jsonPath("$.editable").value(false));
        api.put(participant, base + "/2", Map.of("type", "DESCRIPTION", "description", "Za późno"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WISHLIST_LOCKED"));
    }

    @Test
    void wishlistDeadlineKeepsEditingOpenAfterDraw() throws Exception {
        TestApi.Session admin = api.admin();
        UUID eventId = api.createEvent(admin, Map.of("wishlistDeadline", Instant.now().plusSeconds(86_400).toString()));
        String code = api.openEventWithInvitation(admin, eventId);
        TestApi.Session first = api.participant("Ala");
        TestApi.Session second = api.participant("Ola");
        api.join(first, code);
        api.join(second, code);
        api.closeRegistration(admin, eventId);
        api.draw(admin, eventId).andExpect(status().isOk());

        api.put(second, "/api/events/" + eventId + "/wishlist/1", Map.of("type", "DESCRIPTION", "description", "Książka"))
                .andExpect(status().isOk());
        api.get(first, "/api/events/" + eventId + "/assignment")
                .andExpect(jsonPath("$.recipient.wishlist[0].description").value("Książka"));
    }

    private void insertSuggestion(UUID participantId, int position, String type, String url, String description) {
        jdbc.sql("""
                        INSERT INTO gift_suggestions (id, event_participant_id, position, type, url, description, created_at, updated_at)
                        VALUES (:id, :participant, :position, :type, :url, :description, :now, :now)
                        """)
                .param("id", UUID.randomUUID())
                .param("participant", participantId)
                .param("position", position)
                .param("type", type)
                .param("url", url)
                .param("description", description)
                .param("now", Timestamp.from(Instant.now()))
                .update();
    }
}
