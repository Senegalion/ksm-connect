package com.ksmconnect.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, UUID> {

    List<NotificationOutbox> findByEventIdOrderByCreatedAt(UUID eventId);

    List<NotificationOutbox> findByEventIdAndStatus(UUID eventId, NotificationStatus status);

    @Query("select o.status as status, count(o) as total from NotificationOutbox o where o.eventId = :eventId group by o.status")
    List<StatusCount> countByStatus(UUID eventId);

    interface StatusCount {
        NotificationStatus getStatus();

        long getTotal();
    }
}
