package com.ksmconnect.participant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventParticipantRepository extends JpaRepository<EventParticipant, UUID> {

    Optional<EventParticipant> findByEventIdAndUserId(UUID eventId, UUID userId);

    List<EventParticipant> findByUserId(UUID userId);

    List<EventParticipant> findByEventIdAndStatusOrderById(UUID eventId, ParticipantStatus status);

    long countByEventIdAndStatus(UUID eventId, ParticipantStatus status);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update EventParticipant p set p.assignmentRevealedAt = :now where p.id = :participantId and p.assignmentRevealedAt is null")
    int markAssignmentRevealed(UUID participantId, Instant now);

    @Query("""
            select new com.ksmconnect.participant.ParticipantSummary(
                p.id, u.firstName, u.lastName, u.email, u.phoneNumber, p.status, p.joinedAt)
            from EventParticipant p join com.ksmconnect.user.User u on u.id = p.userId
            where p.eventId = :eventId
            order by u.lastName, u.firstName
            """)
    List<ParticipantSummary> findSummariesByEventId(UUID eventId);

    @Query("""
            select new com.ksmconnect.participant.ParticipantContact(
                p.id, p.eventId, u.firstName, u.lastName, u.email, u.phoneNumber)
            from EventParticipant p join com.ksmconnect.user.User u on u.id = p.userId
            where p.id = :participantId
            """)
    Optional<ParticipantContact> findContact(UUID participantId);
}
