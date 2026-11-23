package com.ksmconnect.participant;

import com.ksmconnect.security.AuthenticatedUser;
import com.ksmconnect.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
class MyEventsController {

    private final ParticipantEventQueries queries;
    private final ParticipationService participationService;

    MyEventsController(ParticipantEventQueries queries, ParticipationService participationService) {
        this.queries = queries;
        this.participationService = participationService;
    }

    @GetMapping
    List<ParticipantEventView> myEvents(@CurrentUser AuthenticatedUser principal) {
        return queries.myEvents(principal.id());
    }

    @GetMapping("/{eventId}")
    ParticipantEventView myEvent(@CurrentUser AuthenticatedUser principal, @PathVariable UUID eventId) {
        return queries.myEvent(eventId, principal.id());
    }

    @PostMapping("/{eventId}/withdraw")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void withdraw(@CurrentUser AuthenticatedUser principal, @PathVariable UUID eventId) {
        participationService.withdraw(eventId, principal.id());
    }
}
