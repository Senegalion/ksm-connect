package com.ksmconnect.admin;

import com.ksmconnect.event.DrawMode;
import com.ksmconnect.event.EventSettings;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

record AdminEventRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 4000) String description,
        @Size(max = 200) String location,
        Instant startsAt,
        @NotBlank @Size(max = 64) String timezone,
        Instant registrationOpensAt,
        Instant registrationClosesAt,
        @NotNull DrawMode drawMode,
        Instant drawAt,
        Instant wishlistDeadline,
        @Min(2) @Max(10_000) Integer participantLimit
) {

    EventSettings toSettings() {
        return new EventSettings(name, description, location, startsAt, timezone, registrationOpensAt,
                registrationClosesAt, drawMode, drawAt, wishlistDeadline, participantLimit);
    }
}
