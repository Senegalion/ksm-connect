package com.ksmconnect.admin;

import com.ksmconnect.common.audit.AuditEventRepository;
import com.ksmconnect.common.error.BusinessRuleException;
import com.ksmconnect.draw.DrawResult;
import com.ksmconnect.draw.DrawService;
import com.ksmconnect.event.Event;
import com.ksmconnect.event.EventAdminService;
import com.ksmconnect.event.EventStatus;
import com.ksmconnect.invitation.InvitationService;
import com.ksmconnect.notification.NotificationAdminService;
import com.ksmconnect.participant.EventParticipantRepository;
import com.ksmconnect.participant.ParticipantStatus;
import com.ksmconnect.participant.ParticipantSummary;
import com.ksmconnect.participant.ParticipationService;
import com.ksmconnect.security.AuthenticatedUser;
import com.ksmconnect.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/events")
class AdminEventController {

    private final EventAdminService eventAdminService;
    private final InvitationService invitationService;
    private final ParticipationService participationService;
    private final EventParticipantRepository participantRepository;
    private final DrawService drawService;
    private final NotificationAdminService notificationAdminService;
    private final AuditEventRepository auditEventRepository;
    private final Clock clock;

    AdminEventController(EventAdminService eventAdminService, InvitationService invitationService,
                         ParticipationService participationService, EventParticipantRepository participantRepository,
                         DrawService drawService, NotificationAdminService notificationAdminService,
                         AuditEventRepository auditEventRepository, Clock clock) {
        this.eventAdminService = eventAdminService;
        this.invitationService = invitationService;
        this.participationService = participationService;
        this.participantRepository = participantRepository;
        this.drawService = drawService;
        this.notificationAdminService = notificationAdminService;
        this.auditEventRepository = auditEventRepository;
        this.clock = clock;
    }

    record DrawRequest(@NotNull Boolean confirmed) {
    }

    record RetryResult(int requeued) {
    }

    record AuditEntryView(String type, Instant createdAt, String details) {
    }

    @GetMapping
    List<AdminEventView> list() {
        return eventAdminService.list().stream().map(this::view).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AdminEventView create(@CurrentUser AuthenticatedUser admin, @Valid @RequestBody AdminEventRequest request) {
        return view(eventAdminService.create(request.toSettings(), admin.id()));
    }

    @GetMapping("/{eventId}")
    AdminEventView get(@PathVariable UUID eventId) {
        return view(eventAdminService.get(eventId));
    }

    @PutMapping("/{eventId}")
    AdminEventView update(@CurrentUser AuthenticatedUser admin, @PathVariable UUID eventId,
                          @Valid @RequestBody AdminEventRequest request) {
        eventAdminService.update(eventId, request.toSettings(), admin.id());
        return get(eventId);
    }

    @PostMapping("/{eventId}/open-registration")
    AdminEventView openRegistration(@CurrentUser AuthenticatedUser admin, @PathVariable UUID eventId) {
        eventAdminService.openRegistration(eventId, admin.id());
        return get(eventId);
    }

    @PostMapping("/{eventId}/close-registration")
    AdminEventView closeRegistration(@CurrentUser AuthenticatedUser admin, @PathVariable UUID eventId) {
        eventAdminService.closeRegistration(eventId, admin.id());
        return get(eventId);
    }

    @PostMapping("/{eventId}/cancel")
    AdminEventView cancel(@CurrentUser AuthenticatedUser admin, @PathVariable UUID eventId) {
        eventAdminService.cancel(eventId, admin.id());
        return get(eventId);
    }

    @PostMapping("/{eventId}/invitation")
    ResponseEntity<InvitationService.GeneratedInvitation> regenerateInvitation(
            @CurrentUser AuthenticatedUser admin, @PathVariable UUID eventId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(invitationService.regenerate(eventId, admin.id()));
    }

    @GetMapping("/{eventId}/participants")
    List<ParticipantSummary> participants(@PathVariable UUID eventId) {
        eventAdminService.get(eventId);
        return participantRepository.findSummariesByEventId(eventId);
    }

    @DeleteMapping("/{eventId}/participants/{participantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeParticipant(@CurrentUser AuthenticatedUser admin, @PathVariable UUID eventId,
                           @PathVariable UUID participantId) {
        participationService.removeByAdmin(eventId, participantId, admin.id());
    }

    @PostMapping("/{eventId}/draw")
    DrawResult draw(@CurrentUser AuthenticatedUser admin, @PathVariable UUID eventId,
                    @Valid @RequestBody DrawRequest request) {
        if (!Boolean.TRUE.equals(request.confirmed())) {
            throw new BusinessRuleException("DRAW_CONFIRMATION_REQUIRED", "The draw must be explicitly confirmed");
        }
        return drawService.drawManually(eventId, admin.id());
    }

    @GetMapping("/{eventId}/notifications")
    List<NotificationAdminService.DeliveryStatusView> notifications(@PathVariable UUID eventId) {
        eventAdminService.get(eventId);
        return notificationAdminService.list(eventId);
    }

    @PostMapping("/{eventId}/notifications/{notificationId}/retry")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void retryNotification(@CurrentUser AuthenticatedUser admin, @PathVariable UUID eventId,
                           @PathVariable UUID notificationId) {
        notificationAdminService.retry(eventId, notificationId, admin.id());
    }

    @PostMapping("/{eventId}/notifications/retry-failed")
    RetryResult retryFailed(@CurrentUser AuthenticatedUser admin, @PathVariable UUID eventId) {
        return new RetryResult(notificationAdminService.retryAllFailed(eventId, admin.id()));
    }

    @GetMapping("/{eventId}/audit")
    List<AuditEntryView> audit(@PathVariable UUID eventId) {
        eventAdminService.get(eventId);
        return auditEventRepository.findByEventIdOrderByCreatedAtDesc(eventId).stream()
                .map(entry -> new AuditEntryView(entry.getType(), entry.getCreatedAt(), entry.getDetails()))
                .toList();
    }

    private AdminEventView view(Event event) {
        Instant now = clock.instant();
        boolean drawOverdue = event.getStatus() != EventStatus.DRAW_COMPLETED
                && event.getStatus() != EventStatus.CANCELLED
                && event.isScheduledDrawDue(now);
        return AdminEventView.of(event,
                participantRepository.countByEventIdAndStatus(event.getId(), ParticipantStatus.ACTIVE),
                drawOverdue,
                invitationService.status(event.getId()),
                notificationAdminService.summary(event.getId()));
    }
}
