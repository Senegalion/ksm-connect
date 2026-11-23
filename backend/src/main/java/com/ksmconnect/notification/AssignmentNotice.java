package com.ksmconnect.notification;

import java.time.Instant;
import java.util.UUID;

public record AssignmentNotice(
        UUID eventId,
        String eventName,
        Instant eventStartsAt,
        String eventTimezone,
        String eventLocation,
        String giverFirstName,
        String giverEmail,
        String giverPhoneNumber,
        String recipientFirstName,
        String recipientFullName
) {

    @Override
    public String toString() {
        return "AssignmentNotice[eventId=" + eventId + "]";
    }
}
