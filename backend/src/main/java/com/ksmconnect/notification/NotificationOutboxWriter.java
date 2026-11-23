package com.ksmconnect.notification;

import com.ksmconnect.config.KsmProperties;
import com.ksmconnect.participant.ParticipantContact;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Component
public class NotificationOutboxWriter {

    private final NotificationOutboxRepository repository;
    private final KsmProperties properties;
    private final Clock clock;

    NotificationOutboxWriter(NotificationOutboxRepository repository, KsmProperties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public int enqueueAssignmentReady(UUID eventId, Collection<ParticipantContact> recipients) {
        Instant now = clock.instant();
        int maxAttempts = properties.notification().maxAttempts();
        List<NotificationOutbox> entries = new ArrayList<>();
        for (ParticipantContact contact : recipients) {
            entries.add(new NotificationOutbox(eventId, contact.participantId(), NotificationChannel.EMAIL,
                    NotificationType.ASSIGNMENT_READY, maxAttempts, now));
            if (properties.notification().smsEnabled() && contact.phoneNumber() != null) {
                entries.add(new NotificationOutbox(eventId, contact.participantId(), NotificationChannel.SMS,
                        NotificationType.ASSIGNMENT_READY, maxAttempts, now));
            }
        }
        repository.saveAll(entries);
        return entries.size();
    }
}
