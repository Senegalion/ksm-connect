package com.ksmconnect.security;

import com.ksmconnect.common.error.TooManyRequestsException;
import com.ksmconnect.config.KsmProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimiter {

    private static final int MAX_TRACKED_KEYS = 100_000;

    private final Map<String, TrackedBucket> buckets = new ConcurrentHashMap<>();
    private final boolean enabled;
    private final Clock clock;

    public RateLimiter(KsmProperties properties, Clock clock) {
        this.enabled = properties.security().rateLimitingEnabled();
        this.clock = clock;
    }

    public void consume(RateLimitPolicy policy, String key) {
        if (!tryConsume(policy, key)) {
            throw new TooManyRequestsException("RATE_LIMITED", "Too many requests");
        }
    }

    public boolean tryConsume(RateLimitPolicy policy, String key) {
        if (!enabled) {
            return true;
        }
        if (buckets.size() > MAX_TRACKED_KEYS) {
            evictIdle();
        }
        TrackedBucket tracked = buckets.computeIfAbsent(policy.name() + ":" + key, ignored -> newBucket(policy));
        tracked.touch(clock.instant());
        return tracked.bucket().tryConsume(1);
    }

    @Scheduled(fixedDelayString = "PT10M")
    void evictIdle() {
        Instant threshold = clock.instant().minus(java.time.Duration.ofHours(2));
        buckets.entrySet().removeIf(entry -> entry.getValue().lastAccess().isBefore(threshold));
    }

    private TrackedBucket newBucket(RateLimitPolicy policy) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(policy.capacity())
                .refillGreedy(policy.capacity(), policy.window())
                .build();
        return new TrackedBucket(Bucket.builder().addLimit(limit).build(), clock.instant());
    }

    private static final class TrackedBucket {
        private final Bucket bucket;
        private volatile Instant lastAccess;

        TrackedBucket(Bucket bucket, Instant lastAccess) {
            this.bucket = bucket;
            this.lastAccess = lastAccess;
        }

        Bucket bucket() {
            return bucket;
        }

        Instant lastAccess() {
            return lastAccess;
        }

        void touch(Instant now) {
            lastAccess = now;
        }
    }
}
