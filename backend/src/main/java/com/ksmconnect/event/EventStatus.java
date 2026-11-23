package com.ksmconnect.event;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum EventStatus {
    DRAFT,
    REGISTRATION_OPEN,
    REGISTRATION_CLOSED,
    DRAW_COMPLETED,
    CANCELLED;

    private static final Map<EventStatus, Set<EventStatus>> ALLOWED_TRANSITIONS = Map.of(
            DRAFT, EnumSet.of(REGISTRATION_OPEN, CANCELLED),
            REGISTRATION_OPEN, EnumSet.of(REGISTRATION_CLOSED, CANCELLED),
            REGISTRATION_CLOSED, EnumSet.of(REGISTRATION_OPEN, DRAW_COMPLETED, CANCELLED),
            DRAW_COMPLETED, EnumSet.noneOf(EventStatus.class),
            CANCELLED, EnumSet.noneOf(EventStatus.class));

    public boolean canTransitionTo(EventStatus target) {
        return ALLOWED_TRANSITIONS.get(this).contains(target);
    }

    public boolean isConfigurable() {
        return this == DRAFT || this == REGISTRATION_OPEN || this == REGISTRATION_CLOSED;
    }
}
