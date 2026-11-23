package com.ksmconnect.invitation;

import com.ksmconnect.participant.ParticipantEventQueries;
import com.ksmconnect.participant.ParticipantEventView;
import com.ksmconnect.participant.ParticipationService;
import com.ksmconnect.security.AuthenticatedUser;
import com.ksmconnect.security.ClientIpResolver;
import com.ksmconnect.security.CurrentUser;
import com.ksmconnect.security.RateLimitPolicy;
import com.ksmconnect.security.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/invitations")
class InvitationController {

    private final InvitationService invitationService;
    private final ParticipationService participationService;
    private final ParticipantEventQueries eventQueries;
    private final RateLimiter rateLimiter;
    private final ClientIpResolver clientIpResolver;

    InvitationController(InvitationService invitationService, ParticipationService participationService,
                         ParticipantEventQueries eventQueries, RateLimiter rateLimiter,
                         ClientIpResolver clientIpResolver) {
        this.invitationService = invitationService;
        this.participationService = participationService;
        this.eventQueries = eventQueries;
        this.rateLimiter = rateLimiter;
        this.clientIpResolver = clientIpResolver;
    }

    record InvitationCodeRequest(@NotBlank @Size(max = 64) String code) {
        @Override
        public String toString() {
            return "InvitationCodeRequest[redacted]";
        }
    }

    record JoinResponse(ParticipantEventView event, boolean newlyJoined) {
    }

    @PostMapping("/preview")
    ParticipantEventView preview(@CurrentUser AuthenticatedUser principal,
                                 @Valid @RequestBody InvitationCodeRequest request, HttpServletRequest httpRequest) {
        UUID eventId = resolve(principal, request, httpRequest);
        return eventQueries.invitationPreview(eventId, principal.id());
    }

    @PostMapping("/accept")
    JoinResponse accept(@CurrentUser AuthenticatedUser principal,
                        @Valid @RequestBody InvitationCodeRequest request, HttpServletRequest httpRequest) {
        UUID eventId = resolve(principal, request, httpRequest);
        ParticipationService.JoinResult result = participationService.join(eventId, principal.id());
        return new JoinResponse(eventQueries.myEvent(eventId, principal.id()), result.newlyJoined());
    }

    private UUID resolve(AuthenticatedUser principal, InvitationCodeRequest request, HttpServletRequest httpRequest) {
        rateLimiter.consume(RateLimitPolicy.INVITATION_PER_USER, principal.id().toString());
        rateLimiter.consume(RateLimitPolicy.INVITATION_PER_IP, clientIpResolver.resolve(httpRequest));
        return invitationService.resolveEventId(request.code());
    }
}
