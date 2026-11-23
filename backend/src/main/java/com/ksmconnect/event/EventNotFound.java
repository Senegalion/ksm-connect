package com.ksmconnect.event;

import com.ksmconnect.common.error.NotFoundException;

public final class EventNotFound {

    private EventNotFound() {
    }

    public static NotFoundException exception() {
        return new NotFoundException("EVENT_NOT_FOUND", "Event not found");
    }
}
