package com.ksmconnect.draw;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

interface AssignmentRepository extends Repository<Assignment, UUID> {

    Assignment save(Assignment assignment);

    void flush();

    Optional<Assignment> findByEventIdAndGiverParticipantId(UUID eventId, UUID giverParticipantId);

    Optional<Assignment> findByGiverParticipantId(UUID giverParticipantId);

    long countByEventId(UUID eventId);

    @Query("""
            select count(a) > 0 from Assignment a, com.ksmconnect.participant.EventParticipant p
            where p.userId = :userId
              and (a.giverParticipantId = p.id or a.recipientParticipantId = p.id)
            """)
    boolean existsInvolvingUser(UUID userId);
}
