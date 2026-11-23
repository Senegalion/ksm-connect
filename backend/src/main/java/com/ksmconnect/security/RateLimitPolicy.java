package com.ksmconnect.security;

import java.time.Duration;

public enum RateLimitPolicy {
    API_PER_IP(600, Duration.ofMinutes(1)),
    LOGIN_PER_IP(40, Duration.ofMinutes(5)),
    LOGIN_PER_ACCOUNT(10, Duration.ofMinutes(15)),
    REGISTRATION_PER_IP(40, Duration.ofHours(1)),
    EMAIL_REQUEST_PER_IP(20, Duration.ofHours(1)),
    EMAIL_REQUEST_PER_ACCOUNT(3, Duration.ofHours(1)),
    INVITATION_PER_USER(10, Duration.ofMinutes(15)),
    INVITATION_PER_IP(60, Duration.ofMinutes(15));

    private final long capacity;
    private final Duration window;

    RateLimitPolicy(long capacity, Duration window) {
        this.capacity = capacity;
        this.window = window;
    }

    public long capacity() {
        return capacity;
    }

    public Duration window() {
        return window;
    }
}
