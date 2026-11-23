package com.ksmconnect.security;

import com.ksmconnect.common.error.TooManyRequestsException;
import com.ksmconnect.config.KsmProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimiterTest {

    @Test
    void blocksAfterCapacityPerKey() {
        RateLimiter limiter = new RateLimiter(properties(true), Clock.systemUTC());
        long capacity = RateLimitPolicy.INVITATION_PER_USER.capacity();
        for (int i = 0; i < capacity; i++) {
            limiter.consume(RateLimitPolicy.INVITATION_PER_USER, "user-1");
        }
        assertThatThrownBy(() -> limiter.consume(RateLimitPolicy.INVITATION_PER_USER, "user-1"))
                .isInstanceOf(TooManyRequestsException.class);
        assertThat(limiter.tryConsume(RateLimitPolicy.INVITATION_PER_USER, "user-2")).isTrue();
        assertThat(limiter.tryConsume(RateLimitPolicy.LOGIN_PER_IP, "user-1")).isTrue();
    }

    @Test
    void disabledLimiterAlwaysAllows() {
        RateLimiter limiter = new RateLimiter(properties(false), Clock.systemUTC());
        for (int i = 0; i < 1000; i++) {
            limiter.consume(RateLimitPolicy.INVITATION_PER_USER, "user-1");
        }
    }

    private static KsmProperties properties(boolean enabled) {
        return new KsmProperties("http://localhost", "x".repeat(32), "v1", "Europe/Warsaw",
                new KsmProperties.Security("", List.of(), enabled),
                new KsmProperties.Auth(Duration.ofHours(24), Duration.ofHours(1), 5, Duration.ofMinutes(15)),
                new KsmProperties.Notification("KSM <a@b.c>", "none", "", "https://api.smsapi.pl", "", 8,
                        Duration.ofSeconds(30), Duration.ofHours(2), 20, Duration.ofMinutes(5)),
                new KsmProperties.Scheduling(false),
                new KsmProperties.Retention(90),
                new KsmProperties.Email("smtp", "", "", ""));
    }
}
