package com.ksmconnect.event;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.id = :id")
    Optional<Event> findByIdForUpdate(UUID id);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select e from Event e where e.id = :id")
    Optional<Event> findByIdForShare(UUID id);

    List<Event> findAllByOrderByCreatedAtDesc();

    @Query("""
            select e.id from Event e
            where e.status = com.ksmconnect.event.EventStatus.REGISTRATION_OPEN
              and ((e.registrationClosesAt is not null and e.registrationClosesAt <= :now)
                   or (e.drawMode = com.ksmconnect.event.DrawMode.SCHEDULED and e.drawAt <= :now))
            """)
    List<UUID> findIdsWithExpiredRegistration(Instant now);

    @Query("""
            select e.id from Event e
            where e.status = com.ksmconnect.event.EventStatus.REGISTRATION_CLOSED
              and e.drawMode = com.ksmconnect.event.DrawMode.SCHEDULED
              and e.drawAt <= :now
            """)
    List<UUID> findIdsWithDueScheduledDraw(Instant now);

    @Query("""
            select e.id from Event e
            where e.personalDataPurgedAt is null
              and ((e.status = com.ksmconnect.event.EventStatus.DRAW_COMPLETED
                      and coalesce(e.startsAt, e.drawCompletedAt) < :threshold)
                   or (e.status = com.ksmconnect.event.EventStatus.CANCELLED and e.cancelledAt < :threshold))
            """)
    List<UUID> findIdsDueForPurge(Instant threshold);
}
