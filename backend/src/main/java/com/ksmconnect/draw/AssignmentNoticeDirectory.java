package com.ksmconnect.draw;

import com.ksmconnect.event.Event;
import com.ksmconnect.event.EventRepository;
import com.ksmconnect.notification.AssignmentDirectory;
import com.ksmconnect.notification.AssignmentNotice;
import com.ksmconnect.participant.EventParticipantRepository;
import com.ksmconnect.participant.ParticipantContact;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
class AssignmentNoticeDirectory implements AssignmentDirectory {

    private final AssignmentRepository assignmentRepository;
    private final EventParticipantRepository participantRepository;
    private final EventRepository eventRepository;

    AssignmentNoticeDirectory(AssignmentRepository assignmentRepository,
                              EventParticipantRepository participantRepository, EventRepository eventRepository) {
        this.assignmentRepository = assignmentRepository;
        this.participantRepository = participantRepository;
        this.eventRepository = eventRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AssignmentNotice> noticeForGiver(UUID giverParticipantId) {
        return assignmentRepository.findByGiverParticipantId(giverParticipantId).flatMap(assignment -> {
            Optional<ParticipantContact> giver = participantRepository.findContact(giverParticipantId);
            Optional<ParticipantContact> recipient = participantRepository.findContact(assignment.getRecipientParticipantId());
            Optional<Event> event = eventRepository.findById(assignment.getEventId());
            if (giver.isEmpty() || recipient.isEmpty() || event.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new AssignmentNotice(event.get().getId(), event.get().getName(),
                    event.get().getStartsAt(), event.get().getTimezone(), event.get().getLocation(),
                    giver.get().firstName(), giver.get().email(), giver.get().phoneNumber(),
                    recipient.get().firstName(), recipient.get().fullName()));
        });
    }
}
