package com.ksmconnect.participant;

import com.ksmconnect.event.DrawMode;
import com.ksmconnect.event.Event;
import com.ksmconnect.event.EventStatus;

import java.time.Instant;
import java.util.UUID;

public record ParticipantEventView(
        UUID id,
        String name,
        String description,
        String location,
        Instant startsAt,
        String timezone,
        Instant registrationOpensAt,
        Instant registrationClosesAt,
        DrawMode drawMode,
        Instant drawAt,
        Instant wishlistDeadline,
        EventStatus status,
        long participantCount,
        Participation participation
) {

    public record Participation(ParticipantStatus status, Instant joinedAt, boolean wishlistEditable) {
    }

    public static ParticipantEventView of(Event event, long participantCount, Participation participation) {
        return new ParticipantEventView(event.getId(), event.getName(), event.getDescription(), event.getLocation(),
                event.getStartsAt(), event.getTimezone(), event.getRegistrationOpensAt(),
                event.getRegistrationClosesAt(), event.getDrawMode(), event.getDrawAt(), event.getWishlistDeadline(),
                event.getStatus(), participantCount, participation);
    }
}
