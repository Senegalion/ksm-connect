package com.ksmconnect.draw;

import java.time.Instant;
import java.util.UUID;

public record DrawResult(UUID eventId, int participantCount, Instant completedAt) {
}
