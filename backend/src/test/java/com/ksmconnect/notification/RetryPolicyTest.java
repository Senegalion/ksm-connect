package com.ksmconnect.notification;

import com.ksmconnect.config.KsmProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RetryPolicyTest {

    private final RetryPolicy policy = new RetryPolicy(properties(Duration.ofSeconds(30), Duration.ofHours(2)));

    @Test
    void backoffGrowsExponentiallyWithJitter() {
        assertThat(policy.backoffFor(1)).isBetween(Duration.ofSeconds(24), Duration.ofSeconds(36));
        assertThat(policy.backoffFor(2)).isBetween(Duration.ofSeconds(48), Duration.ofSeconds(72));
        assertThat(policy.backoffFor(4)).isBetween(Duration.ofSeconds(192), Duration.ofSeconds(288));
    }

    @Test
    void backoffIsCappedAtMaximum() {
        assertThat(policy.backoffFor(30)).isLessThanOrEqualTo(Duration.ofHours(2));
        assertThat(policy.backoffFor(Integer.MAX_VALUE)).isLessThanOrEqualTo(Duration.ofHours(2));
    }

    static KsmProperties properties(Duration initial, Duration max) {
        return new KsmProperties("http://localhost", "x".repeat(32), "v1", "Europe/Warsaw",
                new KsmProperties.Security("", List.of(), true),
                new KsmProperties.Auth(Duration.ofHours(24), Duration.ofHours(1), 5, Duration.ofMinutes(15)),
                new KsmProperties.Notification("KSM <a@b.c>", "none", "", "https://api.smsapi.pl", "", 8,
                        initial, max, 20, Duration.ofMinutes(5)),
                new KsmProperties.Scheduling(false),
                new KsmProperties.Retention(90),
                new KsmProperties.Email("smtp", "", "", ""));
    }
}
