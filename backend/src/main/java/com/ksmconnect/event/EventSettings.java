package com.ksmconnect.event;

import com.ksmconnect.common.error.BusinessRuleException;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;

public record EventSettings(
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
        Integer participantLimit
) {

    public EventSettings {
        name = name == null ? null : name.strip();
        description = blankToNull(description);
        location = blankToNull(location);
        validate(timezone, registrationOpensAt, registrationClosesAt, drawMode, drawAt, participantLimit);
    }

    private static void validate(String timezone, Instant opensAt, Instant closesAt, DrawMode drawMode,
                                 Instant drawAt, Integer participantLimit) {
        try {
            ZoneId.of(timezone);
        } catch (DateTimeException | NullPointerException exception) {
            throw rule("INVALID_TIMEZONE");
        }
        if (opensAt != null && closesAt != null && !closesAt.isAfter(opensAt)) {
            throw rule("INVALID_REGISTRATION_WINDOW");
        }
        if (drawMode == DrawMode.SCHEDULED && drawAt == null) {
            throw rule("DRAW_TIME_REQUIRED");
        }
        if (drawAt != null && closesAt != null && drawAt.isBefore(closesAt)) {
            throw rule("DRAW_BEFORE_REGISTRATION_CLOSE");
        }
        if (participantLimit != null && (participantLimit < 2 || participantLimit > 10_000)) {
            throw rule("INVALID_PARTICIPANT_LIMIT");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static BusinessRuleException rule(String code) {
        return new BusinessRuleException(code, "Invalid event settings: " + code);
    }
}
