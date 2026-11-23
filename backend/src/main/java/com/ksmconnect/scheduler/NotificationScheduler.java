package com.ksmconnect.scheduler;

import com.ksmconnect.config.KsmProperties;
import com.ksmconnect.notification.OutboxWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);
    private static final int MAX_BATCHES_PER_TICK = 10;

    private final OutboxWorker worker;
    private final int batchSize;

    NotificationScheduler(OutboxWorker worker, KsmProperties properties) {
        this.worker = worker;
        this.batchSize = properties.notification().batchSize();
    }

    @Scheduled(fixedDelayString = "${ksm.scheduling.outbox-interval:PT10S}", initialDelayString = "PT10S")
    void tick() {
        try {
            for (int batch = 0; batch < MAX_BATCHES_PER_TICK; batch++) {
                if (worker.processDueNotifications() < batchSize) {
                    return;
                }
            }
        } catch (RuntimeException exception) {
            log.error("Outbox processing failed: {}", exception.getClass().getSimpleName(), exception);
        }
    }
}
