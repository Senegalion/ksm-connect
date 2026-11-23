package com.ksmconnect.notification;

import com.ksmconnect.common.audit.AuditLog;
import com.ksmconnect.common.audit.AuditType;
import com.ksmconnect.common.error.ConflictException;
import com.ksmconnect.common.error.NotFoundException;
import com.ksmconnect.participant.EventParticipantRepository;
import com.ksmconnect.participant.ParticipantContact;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationAdminService {

    private final NotificationOutboxRepository repository;
    private final EventParticipantRepository participantRepository;
    private final AuditLog auditLog;
    private final Clock clock;

    NotificationAdminService(NotificationOutboxRepository repository, EventParticipantRepository participantRepository,
                             AuditLog auditLog, Clock clock) {
        this.repository = repository;
        this.participantRepository = participantRepository;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    public record DeliveryStatusView(UUID id, UUID participantId, String participantName, NotificationChannel channel,
                                     NotificationStatus status, int attemptCount, int maxAttempts,
                                     Instant nextAttemptAt, Instant sentAt, String lastError) {
    }

    @Transactional(readOnly = true)
    public List<DeliveryStatusView> list(UUID eventId) {
        return repository.findByEventIdOrderByCreatedAt(eventId).stream()
                .map(entry -> new DeliveryStatusView(entry.getId(), entry.getParticipantId(),
                        participantRepository.findContact(entry.getParticipantId())
                                .map(ParticipantContact::fullName).orElse(null),
                        entry.getChannel(), entry.getStatus(), entry.getAttemptCount(), entry.getMaxAttempts(),
                        entry.getNextAttemptAt(), entry.getSentAt(), entry.getLastError()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<NotificationStatus, Long> summary(UUID eventId) {
        Map<NotificationStatus, Long> summary = new EnumMap<>(NotificationStatus.class);
        for (NotificationStatus status : NotificationStatus.values()) {
            summary.put(status, 0L);
        }
        repository.countByStatus(eventId).forEach(count -> summary.put(count.getStatus(), count.getTotal()));
        return summary;
    }

    @Transactional
    public void retry(UUID eventId, UUID notificationId, UUID adminId) {
        NotificationOutbox entry = repository.findById(notificationId)
                .filter(candidate -> candidate.getEventId().equals(eventId))
                .orElseThrow(() -> new NotFoundException("NOTIFICATION_NOT_FOUND", "Notification not found"));
        if (!entry.requeue(clock.instant())) {
            throw new ConflictException("NOTIFICATION_NOT_RETRYABLE", "Notification cannot be retried in its current state");
        }
        auditLog.record(AuditType.NOTIFICATION_RETRY_REQUESTED, adminId, eventId, "notification=" + notificationId);
    }

    @Transactional
    public int retryAllFailed(UUID eventId, UUID adminId) {
        List<NotificationOutbox> failed = repository.findByEventIdAndStatus(eventId, NotificationStatus.FAILED);
        Instant now = clock.instant();
        failed.forEach(entry -> entry.requeue(now));
        if (!failed.isEmpty()) {
            auditLog.record(AuditType.NOTIFICATION_RETRY_REQUESTED, adminId, eventId, "count=" + failed.size());
        }
        return failed.size();
    }
}
