package com.ksmconnect.admin;

import com.ksmconnect.event.DrawMode;
import com.ksmconnect.event.Event;
import com.ksmconnect.event.EventStatus;
import com.ksmconnect.event.RegistrationState;
import com.ksmconnect.invitation.InvitationService;
import com.ksmconnect.notification.NotificationStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

record AdminEventView(
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
        Integer participantLimit,
        EventStatus status,
        Instant drawCompletedAt,
        Instant createdAt,
        Instant updatedAt,
        long activeParticipants,
        boolean drawOverdue,
        RegistrationState registrationState,
        InvitationService.InvitationStatus invitation,
        Map<NotificationStatus, Long> notifications
) {

    static AdminEventView of(Event event, long activeParticipants, boolean drawOverdue,
                             RegistrationState registrationState,
                             InvitationService.InvitationStatus invitation,
                             Map<NotificationStatus, Long> notifications) {
        return new AdminEventView(event.getId(), event.getName(), event.getDescription(), event.getLocation(),
                event.getStartsAt(), event.getTimezone(), event.getRegistrationOpensAt(),
                event.getRegistrationClosesAt(), event.getDrawMode(), event.getDrawAt(), event.getWishlistDeadline(),
                event.getParticipantLimit(), event.getStatus(), event.getDrawCompletedAt(), event.getCreatedAt(),
                event.getUpdatedAt(), activeParticipants, drawOverdue, registrationState, invitation, notifications);
    }
}
