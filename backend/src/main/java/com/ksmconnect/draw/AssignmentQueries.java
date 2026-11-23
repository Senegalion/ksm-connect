package com.ksmconnect.draw;

import com.ksmconnect.event.Event;
import com.ksmconnect.event.EventNotFound;
import com.ksmconnect.event.EventRepository;
import com.ksmconnect.participant.EventParticipant;
import com.ksmconnect.participant.EventParticipantRepository;
import com.ksmconnect.participant.ParticipantContact;
import com.ksmconnect.participant.ParticipationService;
import com.ksmconnect.wishlist.WishlistService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
class AssignmentQueries {

    private final ParticipationService participationService;
    private final EventRepository eventRepository;
    private final AssignmentRepository assignmentRepository;
    private final EventParticipantRepository participantRepository;
    private final WishlistService wishlistService;

    AssignmentQueries(ParticipationService participationService, EventRepository eventRepository,
                      AssignmentRepository assignmentRepository, EventParticipantRepository participantRepository,
                      WishlistService wishlistService) {
        this.participationService = participationService;
        this.eventRepository = eventRepository;
        this.assignmentRepository = assignmentRepository;
        this.participantRepository = participantRepository;
        this.wishlistService = wishlistService;
    }

    @Transactional(readOnly = true)
    AssignmentView myAssignment(UUID eventId, UUID userId) {
        EventParticipant giver = participationService.requireActiveParticipant(eventId, userId);
        Event event = eventRepository.findById(eventId).orElseThrow(EventNotFound::exception);
        if (!event.isDrawCompleted()) {
            return AssignmentView.pending(event.getStatus(), event.getDrawAt());
        }
        Assignment assignment = assignmentRepository.findByEventIdAndGiverParticipantId(eventId, giver.getId())
                .orElseThrow(EventNotFound::exception);
        ParticipantContact recipient = participantRepository.findContact(assignment.getRecipientParticipantId())
                .orElseThrow(EventNotFound::exception);
        return new AssignmentView(event.getStatus(), true, event.getDrawAt(),
                new AssignmentView.Recipient(recipient.firstName(), recipient.lastName(),
                        wishlistService.itemsOf(recipient.participantId())));
    }
}
