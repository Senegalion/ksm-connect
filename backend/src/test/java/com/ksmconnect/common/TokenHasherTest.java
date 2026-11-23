package com.ksmconnect.common;

import com.ksmconnect.common.token.SecureTokenGenerator;
import com.ksmconnect.common.token.TokenHasher;
import com.ksmconnect.config.KsmProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenHasherTest {

    @Test
    void hashIsDeterministicAndPepperDependent() {
        TokenHasher first = new TokenHasher(properties("a".repeat(32)));
        TokenHasher second = new TokenHasher(properties("b".repeat(32)));
        assertThat(first.hash("token")).isEqualTo(first.hash("token")).hasSize(64);
        assertThat(first.hash("token")).isNotEqualTo(second.hash("token"));
        assertThat(first.hash("token")).doesNotContain("token");
    }

    @Test
    void rejectsShortPepper() {
        assertThatThrownBy(() -> new TokenHasher(properties("short"))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void urlSafeTokensAreUniqueAndUrlSafe() {
        SecureTokenGenerator generator = new SecureTokenGenerator();
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            String token = generator.urlSafeToken();
            assertThat(token).matches("[A-Za-z0-9_-]{43}");
            tokens.add(token);
        }
        assertThat(tokens).hasSize(10_000);
    }

    private static KsmProperties properties(String pepper) {
        return new KsmProperties("http://localhost", pepper, "v1", "Europe/Warsaw",
                new KsmProperties.Security("", List.of(), true),
                new KsmProperties.Auth(Duration.ofHours(24), Duration.ofHours(1), 5, Duration.ofMinutes(15)),
                new KsmProperties.Notification("KSM <a@b.c>", "none", "", "https://api.smsapi.pl", "", 8,
                        Duration.ofSeconds(30), Duration.ofHours(2), 20, Duration.ofMinutes(5)),
                new KsmProperties.Scheduling(false),
                new KsmProperties.Retention(90));
    }
}
