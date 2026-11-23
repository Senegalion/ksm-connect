package com.ksmconnect.participant;

import com.ksmconnect.common.audit.AuditLog;
import com.ksmconnect.common.audit.AuditType;
import com.ksmconnect.common.error.ConflictException;
import com.ksmconnect.common.error.NotFoundException;
import com.ksmconnect.event.Event;
import com.ksmconnect.event.EventNotFound;
import com.ksmconnect.event.EventRepository;
import com.ksmconnect.event.EventStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class ParticipationService {

    private final EventRepository eventRepository;
    private final EventParticipantRepository participantRepository;
    private final AuditLog auditLog;
    private final Clock clock;

    ParticipationService(EventRepository eventRepository, EventParticipantRepository participantRepository,
                         AuditLog auditLog, Clock clock) {
        this.eventRepository = eventRepository;
        this.participantRepository = participantRepository;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    @Transactional
    public JoinResult join(UUID eventId, UUID userId) {
        Instant now = clock.instant();
        Event event = eventRepository.findByIdForUpdate(eventId).orElseThrow(EventNotFound::exception);
        EventParticipant existing = participantRepository.findByEventIdAndUserId(eventId, userId).orElse(null);
        if (existing != null && existing.isActive()) {
            return new JoinResult(existing, false);
        }
        if (!event.acceptsRegistrations(now)) {
            throw new ConflictException("REGISTRATION_NOT_OPEN", "Registration for this event is not open");
        }
        ensureCapacity(event);
        if (existing != null) {
            existing.rejoin(now);
            return new JoinResult(existing, true);
        }
        return new JoinResult(participantRepository.saveAndFlush(new EventParticipant(eventId, userId, now)), true);
    }

    @Transactional
    public void withdraw(UUID eventId, UUID userId) {
        Event event = eventRepository.findByIdForUpdate(eventId).orElseThrow(EventNotFound::exception);
        EventParticipant participant = participantRepository.findByEventIdAndUserId(eventId, userId)
                .filter(EventParticipant::isActive)
                .orElseThrow(ParticipationService::notParticipating);
        if (event.getStatus() != EventStatus.REGISTRATION_OPEN) {
            throw new ConflictException("WITHDRAWAL_NOT_ALLOWED", "Withdrawal is possible only while registration is open");
        }
        participant.withdraw(clock.instant());
    }

    @Transactional
    public void removeByAdmin(UUID eventId, UUID participantId, UUID adminId) {
        Event event = eventRepository.findByIdForUpdate(eventId).orElseThrow(EventNotFound::exception);
        EventParticipant participant = participantRepository.findById(participantId)
                .filter(candidate -> candidate.getEventId().equals(eventId))
                .filter(EventParticipant::isActive)
                .orElseThrow(ParticipationService::notParticipating);
        if (event.getStatus() != EventStatus.REGISTRATION_OPEN && event.getStatus() != EventStatus.REGISTRATION_CLOSED) {
            throw new ConflictException("REMOVAL_NOT_ALLOWED", "Participants can be removed only before the draw");
        }
        participant.withdraw(clock.instant());
        auditLog.record(AuditType.PARTICIPANT_REMOVED, adminId, eventId, "participant=" + participantId);
    }

    @Transactional(readOnly = true)
    public EventParticipant requireActiveParticipant(UUID eventId, UUID userId) {
        return participantRepository.findByEventIdAndUserId(eventId, userId)
                .filter(EventParticipant::isActive)
                .orElseThrow(ParticipationService::notParticipating);
    }

    private void ensureCapacity(Event event) {
        Integer limit = event.getParticipantLimit();
        if (limit != null && participantRepository.countByEventIdAndStatus(event.getId(), ParticipantStatus.ACTIVE) >= limit) {
            throw new ConflictException("PARTICIPANT_LIMIT_REACHED", "Participant limit reached");
        }
    }

    private static NotFoundException notParticipating() {
        return new NotFoundException("PARTICIPATION_NOT_FOUND", "Participation not found");
    }

    public record JoinResult(EventParticipant participant, boolean newlyJoined) {
    }
}
