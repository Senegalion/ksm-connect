package com.ksmconnect.invitation;

import com.ksmconnect.common.audit.AuditLog;
import com.ksmconnect.common.audit.AuditType;
import com.ksmconnect.common.error.ConflictException;
import com.ksmconnect.common.error.NotFoundException;
import com.ksmconnect.common.token.SecureTokenGenerator;
import com.ksmconnect.common.token.TokenHasher;
import com.ksmconnect.config.KsmProperties;
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
public class InvitationService {

    private final InvitationTokenRepository repository;
    private final EventRepository eventRepository;
    private final SecureTokenGenerator tokenGenerator;
    private final TokenHasher tokenHasher;
    private final AuditLog auditLog;
    private final KsmProperties properties;
    private final Clock clock;

    InvitationService(InvitationTokenRepository repository, EventRepository eventRepository,
                      SecureTokenGenerator tokenGenerator, TokenHasher tokenHasher, AuditLog auditLog,
                      KsmProperties properties, Clock clock) {
        this.repository = repository;
        this.eventRepository = eventRepository;
        this.tokenGenerator = tokenGenerator;
        this.tokenHasher = tokenHasher;
        this.auditLog = auditLog;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public GeneratedInvitation regenerate(UUID eventId, UUID adminId) {
        Instant now = clock.instant();
        Event event = eventRepository.findByIdForUpdate(eventId).orElseThrow(EventNotFound::exception);
        if (event.getStatus() == EventStatus.DRAW_COMPLETED || event.getStatus() == EventStatus.CANCELLED) {
            throw new ConflictException("INVITATION_NOT_ALLOWED", "Invitations cannot be issued for this event");
        }
        repository.revokeActive(eventId, now);
        String canonical = tokenGenerator.fromAlphabet(InvitationCode.ALPHABET, InvitationCode.LENGTH);
        InvitationToken token = repository.save(new InvitationToken(eventId, tokenHasher.hash(canonical), adminId,
                event.getRegistrationClosesAt()));
        auditLog.record(AuditType.INVITATION_GENERATED, adminId, eventId, null);
        String code = InvitationCode.format(canonical);
        return new GeneratedInvitation(code, properties.frontendBaseUrl() + "/join#" + code, token.getExpiresAt());
    }

    @Transactional(readOnly = true)
    public InvitationStatus status(UUID eventId) {
        return repository.findByEventIdAndRevokedAtIsNull(eventId)
                .map(token -> new InvitationStatus(true, token.getCreatedAt(), token.getExpiresAt(),
                        token.isValid(clock.instant())))
                .orElse(new InvitationStatus(false, null, null, false));
    }

    @Transactional(readOnly = true)
    public UUID resolveEventId(String rawCode) {
        Instant now = clock.instant();
        return InvitationCode.canonicalize(rawCode)
                .map(tokenHasher::hash)
                .flatMap(repository::findByTokenHash)
                .filter(token -> token.isValid(now))
                .map(InvitationToken::getEventId)
                .orElseThrow(() -> new NotFoundException("INVALID_INVITATION", "Invitation is invalid or expired"));
    }

    public record GeneratedInvitation(String code, String joinUrl, Instant expiresAt) {
        @Override
        public String toString() {
            return "GeneratedInvitation[redacted]";
        }
    }

    public record InvitationStatus(boolean exists, Instant createdAt, Instant expiresAt, boolean valid) {
    }
}
