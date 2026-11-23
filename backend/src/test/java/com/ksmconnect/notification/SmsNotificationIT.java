package com.ksmconnect.notification;

import com.ksmconnect.support.IntegrationTest;
import com.ksmconnect.support.RecordingSmsSender;
import com.ksmconnect.support.TestApi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@TestPropertySource(properties = "ksm.notification.sms-provider=test")
class SmsNotificationIT {

    @Autowired
    TestApi api;
    @Autowired
    OutboxWorker worker;
    @Autowired
    RecordingSmsSender sms;
    @Autowired
    JdbcClient jdbc;

    @Test
    void sendsPolishSmsWithRecipientFirstNameAndLink() throws Exception {
        TestApi.EventScenario scenario = api.closedEventWithParticipants(2);
        api.draw(scenario.admin(), scenario.eventId()).andExpect(status().isOk());
        assertThat(jdbc.sql("SELECT count(*) FROM notification_outbox WHERE event_id = :id AND channel = 'SMS'")
                .param("id", scenario.eventId()).query(Long.class).single()).isEqualTo(2);

        while (worker.processDueNotifications() > 0) {
        }

        List<SmsMessage> messages = sms.all().stream()
                .filter(message -> message.text().contains(scenario.eventId().toString()))
                .toList();
        assertThat(messages).hasSize(2);
        assertThat(messages).allSatisfy(message -> {
            assertThat(message.to()).isEqualTo("+48600100200");
            assertThat(message.text())
                    .startsWith("🎁 KSM Connect – losowanie zakończone!")
                    .contains("Na Wigilii KSM przygotowujesz prezent dla: ")
                    .contains("Szczegóły oraz lista prezentów: https://app.test/events/" + scenario.eventId() + "/assignment")
                    .endsWith("Powodzenia i udanej zabawy! 🎄")
                    .doesNotContain("Testowa");
        });
        List<String> statuses = jdbc.sql("SELECT status FROM notification_outbox WHERE event_id = :id")
                .param("id", scenario.eventId()).query(String.class).list();
        assertThat(statuses).containsOnly("SENT").hasSize(4);
    }
}
