package com.ksmconnect.common.audit;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;

@Component
public class AuditLog {

    private final AuditEventRepository repository;
    private final Clock clock;

    public AuditLog(AuditEventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void record(AuditType type, UUID actorUserId, UUID eventId, String details) {
        repository.save(new AuditEvent(type.name(), actorUserId, eventId, details, clock.instant()));
    }
}
