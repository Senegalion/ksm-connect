package com.ksmconnect.event;

import com.ksmconnect.common.audit.AuditLog;
import com.ksmconnect.common.audit.AuditType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@Service
public class EventAdminService {

    private final EventRepository eventRepository;
    private final AuditLog auditLog;
    private final Clock clock;

    EventAdminService(EventRepository eventRepository, AuditLog auditLog, Clock clock) {
        this.eventRepository = eventRepository;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    @Transactional
    public Event create(EventSettings settings, UUID adminId) {
        Event event = eventRepository.save(new Event(settings, adminId));
        auditLog.record(AuditType.EVENT_CREATED, adminId, event.getId(), null);
        return event;
    }

    @Transactional
    public Event update(UUID eventId, EventSettings settings, UUID adminId) {
        Event event = lock(eventId);
        event.reconfigure(settings);
        auditLog.record(AuditType.EVENT_UPDATED, adminId, eventId, null);
        return event;
    }

    @Transactional
    public Event openRegistration(UUID eventId, UUID adminId) {
        return changeStatus(eventId, adminId, event -> event.openRegistration(clock.instant()));
    }

    @Transactional
    public Event closeRegistration(UUID eventId, UUID adminId) {
        return changeStatus(eventId, adminId, Event::closeRegistration);
    }

    @Transactional
    public Event cancel(UUID eventId, UUID adminId) {
        return changeStatus(eventId, adminId, event -> event.cancel(clock.instant()));
    }

    @Transactional
    public void closeExpiredRegistration(UUID eventId) {
        Event event = lock(eventId);
        if (event.getStatus() == EventStatus.REGISTRATION_OPEN && event.isRegistrationDeadlinePassed(clock.instant())) {
            event.closeRegistration();
            auditLog.record(AuditType.EVENT_STATUS_CHANGED, null, eventId, "status=REGISTRATION_CLOSED;trigger=SCHEDULER");
        }
    }

    @Transactional(readOnly = true)
    public List<Event> list() {
        return eventRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Event get(UUID eventId) {
        return eventRepository.findById(eventId).orElseThrow(EventNotFound::exception);
    }

    private Event changeStatus(UUID eventId, UUID adminId, Consumer<Event> change) {
        Event event = lock(eventId);
        change.accept(event);
        auditLog.record(AuditType.EVENT_STATUS_CHANGED, adminId, eventId, "status=" + event.getStatus());
        return event;
    }

    private Event lock(UUID eventId) {
        return eventRepository.findByIdForUpdate(eventId).orElseThrow(EventNotFound::exception);
    }
}
