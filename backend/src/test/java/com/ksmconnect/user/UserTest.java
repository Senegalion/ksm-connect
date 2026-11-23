package com.ksmconnect.user;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    private static final Instant NOW = Instant.parse("2026-12-01T10:00:00Z");

    @Test
    void locksAfterConfiguredFailuresAndUnlocksAfterDuration() {
        User user = user();
        for (int i = 0; i < 4; i++) {
            user.recordFailedLogin(NOW, 5, Duration.ofMinutes(15));
        }
        assertThat(user.isLocked(NOW)).isFalse();
        user.recordFailedLogin(NOW, 5, Duration.ofMinutes(15));
        assertThat(user.isLocked(NOW)).isTrue();
        assertThat(user.isLocked(NOW.plus(Duration.ofMinutes(15)))).isFalse();
    }

    @Test
    void successfulLoginResetsFailures() {
        User user = user();
        user.recordFailedLogin(NOW, 2, Duration.ofMinutes(15));
        user.recordSuccessfulLogin();
        user.recordFailedLogin(NOW, 2, Duration.ofMinutes(15));
        assertThat(user.isLocked(NOW)).isFalse();
    }

    @Test
    void newUserIsParticipantOnlyAndCanGainAdmin() {
        User user = user();
        assertThat(user.getRoles()).containsExactly(Role.PARTICIPANT);
        user.grantRole(Role.ADMIN);
        assertThat(user.getRoles()).containsExactlyInAnyOrder(Role.ADMIN, Role.PARTICIPANT);
    }

    @Test
    void normalizesPersonName() {
        assertThat(new PersonName("  Anna   Maria ", " Kowalska ")).isEqualTo(new PersonName("Anna Maria", "Kowalska"));
    }

    @Test
    void verificationTimestampIsSetOnce() {
        User user = user();
        user.markEmailVerified(NOW);
        user.markEmailVerified(NOW.plusSeconds(60));
        assertThat(user.isEmailVerified()).isTrue();
    }

    private static User user() {
        return new User("anna@example.com", new PersonName("Anna", "Kowalska"), "+48600100200", "hash", "v1", NOW);
    }
}
