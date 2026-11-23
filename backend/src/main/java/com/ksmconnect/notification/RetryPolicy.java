package com.ksmconnect.notification;

import com.ksmconnect.config.KsmProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

@Component
class RetryPolicy {

    private static final double JITTER = 0.2;

    private final Duration initialBackoff;
    private final Duration maxBackoff;

    RetryPolicy(KsmProperties properties) {
        this.initialBackoff = properties.notification().initialBackoff();
        this.maxBackoff = properties.notification().maxBackoff();
    }

    Duration backoffFor(int attempt) {
        int exponent = Math.max(0, Math.min(attempt - 1, 20));
        long base = Math.min(initialBackoff.toMillis() * (1L << exponent), maxBackoff.toMillis());
        double factor = 1 + ThreadLocalRandom.current().nextDouble(-JITTER, JITTER);
        return Duration.ofMillis(Math.min((long) (base * factor), maxBackoff.toMillis()));
    }

    Instant nextAttemptAt(Instant now, int attempt) {
        return now.plus(backoffFor(attempt));
    }
}
