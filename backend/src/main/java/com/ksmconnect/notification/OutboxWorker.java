package com.ksmconnect.notification;

import com.ksmconnect.config.KsmProperties;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Component
public class OutboxWorker {

    private final OutboxClaimer claimer;
    private final NotificationDispatcher dispatcher;
    private final KsmProperties.Notification settings;
    private final Clock clock;

    OutboxWorker(OutboxClaimer claimer, NotificationDispatcher dispatcher, KsmProperties properties, Clock clock) {
        this.claimer = claimer;
        this.dispatcher = dispatcher;
        this.settings = properties.notification();
        this.clock = clock;
    }

    public int processDueNotifications() {
        List<UUID> claimed = claimer.claim(clock.instant(), settings.processingTimeout(), settings.batchSize());
        claimed.forEach(dispatcher::dispatch);
        return claimed.size();
    }
}
