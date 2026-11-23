package com.ksmconnect.participant;

import com.ksmconnect.event.Event;
import com.ksmconnect.event.EventNotFound;
import com.ksmconnect.event.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class ParticipantEventQueries {

    private final EventRepository eventRepository;
    private final EventParticipantRepository participantRepository;
    private final Clock clock;

    ParticipantEventQueries(EventRepository eventRepository, EventParticipantRepository participantRepository,
                            Clock clock) {
        this.eventRepository = eventRepository;
        this.participantRepository = participantRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ParticipantEventView> myEvents(UUID userId) {
        return participantRepository.findByUserId(userId).stream()
                .filter(EventParticipant::isActive)
                .map(participant -> view(eventRepository.findById(participant.getEventId()).orElseThrow(), participant))
                .sorted(Comparator.comparing(ParticipantEventView::name))
                .toList();
    }

    @Transactional(readOnly = true)
    public ParticipantEventView myEvent(UUID eventId, UUID userId) {
        EventParticipant participant = participantRepository.findByEventIdAndUserId(eventId, userId)
                .filter(EventParticipant::isActive)
                .orElseThrow(EventNotFound::exception);
        return view(eventRepository.findById(eventId).orElseThrow(EventNotFound::exception), participant);
    }

    @Transactional(readOnly = true)
    public ParticipantEventView invitationPreview(UUID eventId, UUID userId) {
        Event event = eventRepository.findById(eventId).orElseThrow(EventNotFound::exception);
        EventParticipant participant = participantRepository.findByEventIdAndUserId(eventId, userId)
                .filter(EventParticipant::isActive)
                .orElse(null);
        return participant == null
                ? ParticipantEventView.of(event, activeCount(event), event.registrationState(clock.instant()), null)
                : view(event, participant);
    }

    private ParticipantEventView view(Event event, EventParticipant participant) {
        ParticipantEventView.Participation participation = new ParticipantEventView.Participation(
                participant.getStatus(), participant.getJoinedAt(), event.isWishlistEditable(clock.instant()));
        return ParticipantEventView.of(event, activeCount(event), event.registrationState(clock.instant()), participation);
    }

    private long activeCount(Event event) {
        return participantRepository.countByEventIdAndStatus(event.getId(), ParticipantStatus.ACTIVE);
    }
}
