package com.ksmconnect.draw;

import com.ksmconnect.event.EventStatus;
import com.ksmconnect.wishlist.WishlistView;

import java.time.Instant;
import java.util.List;

public record AssignmentView(EventStatus eventStatus, boolean drawCompleted, Instant drawAt, Recipient recipient) {

    public record Recipient(String firstName, String lastName, List<WishlistView.Item> wishlist) {
    }

    static AssignmentView pending(EventStatus status, Instant drawAt) {
        return new AssignmentView(status, false, drawAt, null);
    }
}
