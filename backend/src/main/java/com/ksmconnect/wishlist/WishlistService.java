package com.ksmconnect.wishlist;

import com.ksmconnect.common.error.BusinessRuleException;
import com.ksmconnect.common.error.ConflictException;
import com.ksmconnect.event.Event;
import com.ksmconnect.event.EventNotFound;
import com.ksmconnect.event.EventRepository;
import com.ksmconnect.participant.EventParticipant;
import com.ksmconnect.participant.ParticipationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class WishlistService {

    private final GiftSuggestionRepository repository;
    private final ParticipationService participationService;
    private final EventRepository eventRepository;
    private final Clock clock;

    WishlistService(GiftSuggestionRepository repository, ParticipationService participationService,
                    EventRepository eventRepository, Clock clock) {
        this.repository = repository;
        this.participationService = participationService;
        this.eventRepository = eventRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public WishlistView myWishlist(UUID eventId, UUID userId) {
        EventParticipant participant = participationService.requireActiveParticipant(eventId, userId);
        return WishlistView.of(event(eventId).isWishlistEditable(clock.instant()),
                repository.findByEventParticipantIdOrderByPosition(participant.getId()));
    }

    @Transactional
    public WishlistView save(UUID eventId, UUID userId, int position, SuggestionType type, String url, String description) {
        validatePosition(position);
        EventParticipant participant = editableParticipation(eventId, userId);
        SuggestionContent content = SuggestionContent.of(type, url, description);
        repository.findByEventParticipantIdAndPosition(participant.getId(), (short) position)
                .ifPresentOrElse(
                        existing -> existing.replaceContent(content),
                        () -> repository.save(new GiftSuggestion(participant.getId(), position, content)));
        repository.flush();
        return WishlistView.of(true, repository.findByEventParticipantIdOrderByPosition(participant.getId()));
    }

    @Transactional
    public WishlistView remove(UUID eventId, UUID userId, int position) {
        validatePosition(position);
        EventParticipant participant = editableParticipation(eventId, userId);
        repository.findByEventParticipantIdAndPosition(participant.getId(), (short) position)
                .ifPresent(repository::delete);
        repository.flush();
        return WishlistView.of(true, repository.findByEventParticipantIdOrderByPosition(participant.getId()));
    }

    @Transactional(readOnly = true)
    public List<WishlistView.Item> itemsOf(UUID eventParticipantId) {
        return repository.findByEventParticipantIdOrderByPosition(eventParticipantId).stream()
                .map(WishlistView.Item::of)
                .toList();
    }

    private EventParticipant editableParticipation(UUID eventId, UUID userId) {
        EventParticipant participant = participationService.requireActiveParticipant(eventId, userId);
        if (!event(eventId).isWishlistEditable(clock.instant())) {
            throw new ConflictException("WISHLIST_LOCKED", "Wishlist can no longer be edited");
        }
        return participant;
    }

    private Event event(UUID eventId) {
        return eventRepository.findById(eventId).orElseThrow(EventNotFound::exception);
    }

    private static void validatePosition(int position) {
        if (position < 1 || position > WishlistView.MAX_ITEMS) {
            throw new BusinessRuleException("INVALID_SUGGESTION_POSITION", "Position must be between 1 and 3");
        }
    }
}
