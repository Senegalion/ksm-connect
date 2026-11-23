package com.ksmconnect.notification;

import com.ksmconnect.config.KsmProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AcsEmailSenderTest {

    @Test
    void buildsClientWithoutContactingAzure() {
        assertThatCode(() -> new AcsEmailSender(properties("https://ksm-test.europe.communication.azure.com", "DoNotReply@example.pl")))
                .doesNotThrowAnyException();
    }

    @Test
    void requiresEndpointAndSender() {
        assertThatThrownBy(() -> new AcsEmailSender(properties("", "")))
                .isInstanceOf(IllegalStateException.class);
    }

    private static KsmProperties properties(String endpoint, String sender) {
        return new KsmProperties("http://localhost", "x".repeat(32), "v1", "Europe/Warsaw",
                new KsmProperties.Security("", List.of(), true),
                new KsmProperties.Auth(Duration.ofHours(24), Duration.ofHours(1), 5, Duration.ofMinutes(15)),
                new KsmProperties.Notification("KSM <a@b.c>", "none", "", "https://api.smsapi.pl", "", 8,
                        Duration.ofSeconds(30), Duration.ofHours(2), 20, Duration.ofMinutes(5)),
                new KsmProperties.Scheduling(false),
                new KsmProperties.Retention(90),
                new KsmProperties.Email("acs", endpoint, sender, ""));
    }
}
