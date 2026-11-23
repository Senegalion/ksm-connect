package com.ksmconnect.event;

import com.ksmconnect.common.error.BusinessRuleException;
import com.ksmconnect.common.error.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventTest {

    private static final Instant NOW = Instant.parse("2026-12-01T12:00:00Z");

    @Test
    void newEventStartsAsDraft() {
        assertThat(event(manualSettings()).getStatus()).isEqualTo(EventStatus.DRAFT);
    }

    @Test
    void followsHappyPathLifecycle() {
        Event event = event(manualSettings());
        event.openRegistration(NOW);
        event.closeRegistration();
        event.completeDraw(NOW);
        assertThat(event.getStatus()).isEqualTo(EventStatus.DRAW_COMPLETED);
        assertThat(event.getDrawCompletedAt()).isEqualTo(NOW);
    }

    @Test
    void cannotDrawBeforeRegistrationIsClosed() {
        Event event = event(manualSettings());
        event.openRegistration(NOW);
        assertThatThrownBy(() -> event.completeDraw(NOW)).isInstanceOf(ConflictException.class);
    }

    @Test
    void completedDrawIsTerminal() {
        Event event = event(manualSettings());
        event.openRegistration(NOW);
        event.closeRegistration();
        event.completeDraw(NOW);
        assertThatThrownBy(() -> event.openRegistration(NOW)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> event.cancel(NOW)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> event.completeDraw(NOW)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> event.reconfigure(manualSettings())).isInstanceOf(ConflictException.class);
    }

    @Test
    void registrationCanBeReopenedBeforeDraw() {
        Event event = event(manualSettings());
        event.openRegistration(NOW);
        event.closeRegistration();
        event.openRegistration(NOW);
        assertThat(event.getStatus()).isEqualTo(EventStatus.REGISTRATION_OPEN);
    }

    @ParameterizedTest
    @EnumSource(EventStatus.class)
    void statusTransitionsMatchDocumentedModel(EventStatus from) {
        for (EventStatus to : EventStatus.values()) {
            boolean expected = switch (from) {
                case DRAFT -> to == EventStatus.REGISTRATION_OPEN || to == EventStatus.CANCELLED;
                case REGISTRATION_OPEN -> to == EventStatus.REGISTRATION_CLOSED || to == EventStatus.CANCELLED;
                case REGISTRATION_CLOSED -> to == EventStatus.REGISTRATION_OPEN || to == EventStatus.DRAW_COMPLETED
                        || to == EventStatus.CANCELLED;
                case DRAW_COMPLETED, CANCELLED -> false;
            };
            assertThat(from.canTransitionTo(to)).as(from + " -> " + to).isEqualTo(expected);
        }
    }

    @Test
    void acceptsRegistrationsOnlyWithinWindow() {
        Event event = event(settings(NOW.minus(Duration.ofDays(1)), NOW.plus(Duration.ofDays(1)), DrawMode.MANUAL, null, null));
        assertThat(event.acceptsRegistrations(NOW)).isFalse();
        event.openRegistration(NOW);
        assertThat(event.acceptsRegistrations(NOW)).isTrue();
        assertThat(event.registrationState(NOW.minus(Duration.ofDays(2)))).isEqualTo(RegistrationState.NOT_STARTED);
        assertThat(event.acceptsRegistrations(NOW.plus(Duration.ofDays(1)))).isFalse();
    }

    @Test
    void scheduledDrawTimeEndsRegistration() {
        Event event = event(settings(null, null, DrawMode.SCHEDULED, NOW, null));
        event.openRegistration(NOW.minusSeconds(60));
        assertThat(event.acceptsRegistrations(NOW.minusSeconds(1))).isTrue();
        assertThat(event.acceptsRegistrations(NOW)).isFalse();
        assertThat(event.isRegistrationDeadlinePassed(NOW)).isTrue();
        assertThat(event.isScheduledDrawDue(NOW)).isTrue();
        assertThat(event.isScheduledDrawDue(NOW.minusSeconds(1))).isFalse();
    }

    @Test
    void manualOpeningWithFutureStartOpensImmediately() {
        Event event = event(settings(NOW.plus(Duration.ofDays(2)), NOW.plus(Duration.ofDays(5)), DrawMode.MANUAL, null, null));
        assertThat(event.registrationState(NOW)).isEqualTo(RegistrationState.NOT_STARTED);
        event.openRegistration(NOW);
        assertThat(event.getRegistrationOpensAt()).isEqualTo(NOW);
        assertThat(event.registrationState(NOW)).isEqualTo(RegistrationState.OPEN);
        assertThat(event.acceptsRegistrations(NOW)).isTrue();
    }

    @Test
    void reopeningAfterCloseAcceptsRegistrationsAgain() {
        Event event = event(settings(null, NOW.plus(Duration.ofDays(1)), DrawMode.MANUAL, null, null));
        event.openRegistration(NOW);
        event.closeRegistration();
        assertThat(event.registrationState(NOW)).isEqualTo(RegistrationState.CLOSED);
        event.openRegistration(NOW);
        assertThat(event.acceptsRegistrations(NOW)).isTrue();
    }

    @Test
    void cannotOpenRegistrationAfterDeadline() {
        Event event = event(settings(null, NOW.minusSeconds(60), DrawMode.MANUAL, null, null));
        assertThatThrownBy(() -> event.openRegistration(NOW))
                .isInstanceOf(ConflictException.class)
                .extracting(exception -> ((ConflictException) exception).getCode())
                .isEqualTo("REGISTRATION_DEADLINE_PASSED");
        assertThat(event.getStatus()).isEqualTo(EventStatus.DRAFT);
    }

    @Test
    void registrationStateReflectsLifecycle() {
        Event event = event(manualSettings());
        assertThat(event.registrationState(NOW)).isEqualTo(RegistrationState.NOT_STARTED);
        event.openRegistration(NOW);
        assertThat(event.registrationState(NOW)).isEqualTo(RegistrationState.OPEN);
        event.cancel(NOW);
        assertThat(event.registrationState(NOW)).isEqualTo(RegistrationState.CLOSED);
    }

    @Test
    void manualEventIsNeverScheduledDue() {
        Event event = event(settings(null, null, DrawMode.MANUAL, NOW, null));
        assertThat(event.isScheduledDrawDue(NOW.plus(Duration.ofDays(365)))).isFalse();
    }

    @Test
    void wishlistLocksAtDrawWhenNoDeadline() {
        Event event = event(manualSettings());
        event.openRegistration(NOW);
        event.closeRegistration();
        assertThat(event.isWishlistEditable(NOW)).isTrue();
        event.completeDraw(NOW);
        assertThat(event.isWishlistEditable(NOW)).isFalse();
    }

    @Test
    void wishlistDeadlineOverridesDrawLock() {
        Event event = event(settings(null, null, DrawMode.MANUAL, null, NOW.plus(Duration.ofDays(3))));
        event.openRegistration(NOW);
        event.closeRegistration();
        event.completeDraw(NOW);
        assertThat(event.isWishlistEditable(NOW)).isTrue();
        assertThat(event.isWishlistEditable(NOW.plus(Duration.ofDays(3)))).isFalse();
    }

    @Test
    void cancelledEventLocksWishlist() {
        Event event = event(manualSettings());
        event.cancel(NOW);
        assertThat(event.isWishlistEditable(NOW)).isFalse();
        assertThat(event.getCancelledAt()).isEqualTo(NOW);
    }

    @Test
    void settingsValidationRejectsInconsistentConfiguration() {
        assertRule(() -> settings(NOW, NOW.minusSeconds(1), DrawMode.MANUAL, null, null), "INVALID_REGISTRATION_WINDOW");
        assertRule(() -> settings(null, null, DrawMode.SCHEDULED, null, null), "DRAW_TIME_REQUIRED");
        assertRule(() -> settings(null, NOW, DrawMode.SCHEDULED, NOW.minusSeconds(1), null), "DRAW_BEFORE_REGISTRATION_CLOSE");
        assertRule(() -> new EventSettings("x", null, null, null, "Mars/Olympus", null, null, DrawMode.MANUAL, null, null, null),
                "INVALID_TIMEZONE");
        assertRule(() -> new EventSettings("x", null, null, null, "Europe/Warsaw", null, null, DrawMode.MANUAL, null, null, 1),
                "INVALID_PARTICIPANT_LIMIT");
        assertRule(() -> event(new EventSettings("  ", null, null, null, "Europe/Warsaw", null, null, DrawMode.MANUAL, null, null, null)),
                "EVENT_NAME_REQUIRED");
    }

    private static void assertRule(Runnable action, String code) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessRuleException.class)
                .extracting(exception -> ((BusinessRuleException) exception).getCode())
                .isEqualTo(code);
    }

    private static Event event(EventSettings settings) {
        return new Event(settings, UUID.randomUUID());
    }

    private static EventSettings manualSettings() {
        return settings(null, null, DrawMode.MANUAL, null, null);
    }

    private static EventSettings settings(Instant opensAt, Instant closesAt, DrawMode mode, Instant drawAt, Instant wishlistDeadline) {
        return new EventSettings("Wigilia KSM 2026", "Opis", "Parafia", null, "Europe/Warsaw", opensAt, closesAt,
                mode, drawAt, wishlistDeadline, null);
    }
}
