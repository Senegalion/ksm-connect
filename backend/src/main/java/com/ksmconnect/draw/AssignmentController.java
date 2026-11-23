package com.ksmconnect.draw;

import com.ksmconnect.security.AuthenticatedUser;
import com.ksmconnect.security.CurrentUser;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
class AssignmentController {

    private final AssignmentQueries queries;

    AssignmentController(AssignmentQueries queries) {
        this.queries = queries;
    }

    @GetMapping("/api/events/{eventId}/assignment")
    ResponseEntity<AssignmentView> myAssignment(@CurrentUser AuthenticatedUser principal, @PathVariable UUID eventId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(queries.myAssignment(eventId, principal.id()));
    }

    @PostMapping("/api/events/{eventId}/assignment/reveal")
    ResponseEntity<AssignmentView> markRevealed(@CurrentUser AuthenticatedUser principal, @PathVariable UUID eventId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(queries.markRevealed(eventId, principal.id()));
    }
}
