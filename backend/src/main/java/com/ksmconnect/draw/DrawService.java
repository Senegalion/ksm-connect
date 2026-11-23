package com.ksmconnect.draw;

import com.ksmconnect.common.audit.AuditLog;
import com.ksmconnect.common.audit.AuditType;
import com.ksmconnect.common.error.BusinessRuleException;
import com.ksmconnect.common.error.ConflictException;
import com.ksmconnect.event.Event;
import com.ksmconnect.event.EventNotFound;
import com.ksmconnect.event.EventRepository;
import com.ksmconnect.event.EventStatus;
import com.ksmconnect.notification.NotificationOutboxWriter;
import com.ksmconnect.participant.EventParticipant;
import com.ksmconnect.participant.EventParticipantRepository;
import com.ksmconnect.participant.ParticipantContact;
import com.ksmconnect.participant.ParticipantStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DrawService {

    private static final Logger log = LoggerFactory.getLogger(DrawService.class);

    private final EventRepository eventRepository;
    private final EventParticipantRepository participantRepository;
    private final AssignmentRepository assignmentRepository;
    private final DerangementGenerator generator;
    private final NotificationOutboxWriter outboxWriter;
    private final AuditLog auditLog;
    private final Clock clock;

    DrawService(EventRepository eventRepository, EventParticipantRepository participantRepository,
                AssignmentRepository assignmentRepository, DerangementGenerator generator,
                NotificationOutboxWriter outboxWriter, AuditLog auditLog, Clock clock) {
        this.eventRepository = eventRepository;
        this.participantRepository = participantRepository;
        this.assignmentRepository = assignmentRepository;
        this.generator = generator;
        this.outboxWriter = outboxWriter;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    @Transactional
    public DrawResult drawManually(UUID eventId, UUID adminId) {
        Event event = lockEvent(eventId);
        if (event.isDrawCompleted()) {
            throw new ConflictException("DRAW_ALREADY_COMPLETED", "The draw for this event has already been completed");
        }
        return execute(event, DrawTrigger.MANUAL, adminId);
    }

    @Transactional
    public Optional<DrawResult> drawIfScheduledAndDue(UUID eventId) {
        Event event = lockEvent(eventId);
        if (event.getStatus() != EventStatus.REGISTRATION_CLOSED || !event.isScheduledDrawDue(clock.instant())) {
            return Optional.empty();
        }
        return Optional.of(execute(event, DrawTrigger.SCHEDULED, null));
    }

    private DrawResult execute(Event event, DrawTrigger trigger, UUID actorId) {
        if (event.getStatus() != EventStatus.REGISTRATION_CLOSED) {
            throw new ConflictException("REGISTRATION_NOT_CLOSED", "Registration must be closed before the draw");
        }
        List<UUID> participantIds = participantRepository
                .findByEventIdAndStatusOrderById(event.getId(), ParticipantStatus.ACTIVE).stream()
                .map(EventParticipant::getId)
                .toList();
        if (participantIds.size() < 2) {
            throw new BusinessRuleException("NOT_ENOUGH_PARTICIPANTS", "At least two participants are required");
        }

        List<Pairing<UUID>> pairings = generator.generate(participantIds);
        pairings.forEach(pairing -> assignmentRepository.save(
                new Assignment(event.getId(), pairing.giver(), pairing.recipient())));
        assignmentRepository.flush();

        List<ParticipantContact> contacts = participantIds.stream()
                .map(id -> participantRepository.findContact(id).orElseThrow())
                .toList();
        int notifications = outboxWriter.enqueueAssignmentReady(event.getId(), contacts);

        Instant completedAt = clock.instant();
        event.completeDraw(completedAt);
        eventRepository.flush();

        auditLog.record(AuditType.DRAW_COMPLETED, actorId, event.getId(),
                "trigger=" + trigger + ";participants=" + participantIds.size() + ";notifications=" + notifications);
        log.info("Draw completed for event {} with {} participants via {}", event.getId(), participantIds.size(), trigger);
        return new DrawResult(event.getId(), participantIds.size(), completedAt);
    }

    private Event lockEvent(UUID eventId) {
        return eventRepository.findByIdForUpdate(eventId).orElseThrow(EventNotFound::exception);
    }
}
