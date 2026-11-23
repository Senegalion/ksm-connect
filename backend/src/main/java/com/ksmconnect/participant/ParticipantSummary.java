package com.ksmconnect.participant;

import java.time.Instant;
import java.util.UUID;

public record ParticipantSummary(UUID participantId, String firstName, String lastName, String email,
                                 String phoneNumber, ParticipantStatus status, Instant joinedAt) {
}
