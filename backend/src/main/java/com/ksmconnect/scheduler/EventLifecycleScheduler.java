package com.ksmconnect.scheduler;

import com.ksmconnect.draw.DrawService;
import com.ksmconnect.event.EventAdminService;
import com.ksmconnect.event.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;

@Component
class EventLifecycleScheduler {

    private static final Logger log = LoggerFactory.getLogger(EventLifecycleScheduler.class);

    private final EventRepository eventRepository;
    private final EventAdminService eventAdminService;
    private final DrawService drawService;
    private final Clock clock;

    EventLifecycleScheduler(EventRepository eventRepository, EventAdminService eventAdminService,
                            DrawService drawService, Clock clock) {
        this.eventRepository = eventRepository;
        this.eventAdminService = eventAdminService;
        this.drawService = drawService;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${ksm.scheduling.lifecycle-interval:PT30S}", initialDelayString = "PT15S")
    void tick() {
        eventRepository.findIdsWithExpiredRegistration(clock.instant()).forEach(this::closeRegistration);
        eventRepository.findIdsWithDueScheduledDraw(clock.instant()).forEach(this::runScheduledDraw);
    }

    private void closeRegistration(UUID eventId) {
        try {
            eventAdminService.closeExpiredRegistration(eventId);
        } catch (RuntimeException exception) {
            log.warn("Closing registration for event {} failed: {}", eventId, exception.getClass().getSimpleName());
        }
    }

    private void runScheduledDraw(UUID eventId) {
        try {
            drawService.drawIfScheduledAndDue(eventId);
        } catch (RuntimeException exception) {
            log.warn("Scheduled draw for event {} did not run: {}", eventId, exception.getMessage());
        }
    }
}
