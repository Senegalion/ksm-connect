package com.ksmconnect.wishlist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface GiftSuggestionRepository extends JpaRepository<GiftSuggestion, UUID> {

    List<GiftSuggestion> findByEventParticipantIdOrderByPosition(UUID eventParticipantId);

    Optional<GiftSuggestion> findByEventParticipantIdAndPosition(UUID eventParticipantId, short position);
}
