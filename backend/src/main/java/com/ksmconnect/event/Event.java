package com.ksmconnect.event;

import com.ksmconnect.common.BaseEntity;
import com.ksmconnect.common.error.BusinessRuleException;
import com.ksmconnect.common.error.ConflictException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "events")
public class Event extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String description;

    private String location;

    private Instant startsAt;

    @Column(nullable = false)
    private String timezone;

    private Instant registrationOpensAt;

    private Instant registrationClosesAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DrawMode drawMode;

    private Instant drawAt;

    private Instant wishlistDeadline;

    private Integer participantLimit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    private Instant drawCompletedAt;

    private Instant cancelledAt;

    private Instant personalDataPurgedAt;

    private UUID createdBy;

    protected Event() {
    }

    public Event(EventSettings settings, UUID createdBy) {
        this.status = EventStatus.DRAFT;
        this.createdBy = createdBy;
        apply(settings);
    }

    public void reconfigure(EventSettings settings) {
        if (!status.isConfigurable()) {
            throw new ConflictException("EVENT_NOT_CONFIGURABLE", "Event can no longer be changed");
        }
        apply(settings);
    }

    public void openRegistration(Instant now) {
        if (isRegistrationDeadlinePassed(now)) {
            throw new ConflictException("REGISTRATION_DEADLINE_PASSED",
                    "Registration deadline has already passed; change the event dates first");
        }
        transitionTo(EventStatus.REGISTRATION_OPEN);
        if (registrationOpensAt != null && registrationOpensAt.isAfter(now)) {
            registrationOpensAt = now;
        }
    }

    public void closeRegistration() {
        transitionTo(EventStatus.REGISTRATION_CLOSED);
    }

    public void cancel(Instant now) {
        transitionTo(EventStatus.CANCELLED);
        cancelledAt = now;
    }

    public void completeDraw(Instant now) {
        transitionTo(EventStatus.DRAW_COMPLETED);
        drawCompletedAt = now;
    }

    public boolean acceptsRegistrations(Instant now) {
        return registrationState(now) == RegistrationState.OPEN;
    }

    public RegistrationState registrationState(Instant now) {
        if (isRegistrationDeadlinePassed(now)) {
            return RegistrationState.CLOSED;
        }
        if (status == EventStatus.DRAFT) {
            return RegistrationState.NOT_STARTED;
        }
        if (status != EventStatus.REGISTRATION_OPEN) {
            return RegistrationState.CLOSED;
        }
        if (registrationOpensAt != null && now.isBefore(registrationOpensAt)) {
            return RegistrationState.NOT_STARTED;
        }
        return RegistrationState.OPEN;
    }

    public boolean isRegistrationDeadlinePassed(Instant now) {
        boolean closingTimePassed = registrationClosesAt != null && !now.isBefore(registrationClosesAt);
        return closingTimePassed || isScheduledDrawDue(now);
    }

    public boolean isScheduledDrawDue(Instant now) {
        return drawMode == DrawMode.SCHEDULED && drawAt != null && !now.isBefore(drawAt);
    }

    public boolean isWishlistEditable(Instant now) {
        if (status == EventStatus.CANCELLED || personalDataPurgedAt != null) {
            return false;
        }
        if (wishlistDeadline != null) {
            return now.isBefore(wishlistDeadline);
        }
        return status != EventStatus.DRAW_COMPLETED;
    }

    public boolean isDrawCompleted() {
        return status == EventStatus.DRAW_COMPLETED;
    }

    public void markPersonalDataPurged(Instant now) {
        personalDataPurgedAt = now;
    }

    private void transitionTo(EventStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new ConflictException("INVALID_EVENT_TRANSITION",
                    "Cannot move event from " + status + " to " + target);
        }
        status = target;
    }

    private void apply(EventSettings settings) {
        if (settings.name() == null || settings.name().isBlank()) {
            throw new BusinessRuleException("EVENT_NAME_REQUIRED", "Event name is required");
        }
        this.name = settings.name();
        this.description = settings.description();
        this.location = settings.location();
        this.startsAt = settings.startsAt();
        this.timezone = settings.timezone();
        this.registrationOpensAt = settings.registrationOpensAt();
        this.registrationClosesAt = settings.registrationClosesAt();
        this.drawMode = settings.drawMode();
        this.drawAt = settings.drawAt();
        this.wishlistDeadline = settings.wishlistDeadline();
        this.participantLimit = settings.participantLimit();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public String getTimezone() {
        return timezone;
    }

    public Instant getRegistrationOpensAt() {
        return registrationOpensAt;
    }

    public Instant getRegistrationClosesAt() {
        return registrationClosesAt;
    }

    public DrawMode getDrawMode() {
        return drawMode;
    }

    public Instant getDrawAt() {
        return drawAt;
    }

    public Instant getWishlistDeadline() {
        return wishlistDeadline;
    }

    public Integer getParticipantLimit() {
        return participantLimit;
    }

    public EventStatus getStatus() {
        return status;
    }

    public Instant getDrawCompletedAt() {
        return drawCompletedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public Instant getPersonalDataPurgedAt() {
        return personalDataPurgedAt;
    }
}
