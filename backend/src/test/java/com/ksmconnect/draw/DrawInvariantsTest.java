package com.ksmconnect.draw;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DrawInvariantsTest {

    private static final List<String> PARTICIPANTS = List.of("a", "b", "c");

    @Test
    void acceptsValidDerangement() {
        assertThatCode(() -> DrawInvariants.verify(PARTICIPANTS, List.of(
                new Pairing<>("a", "b"), new Pairing<>("b", "c"), new Pairing<>("c", "a"))))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsSelfAssignment() {
        assertViolation(List.of(new Pairing<>("a", "a"), new Pairing<>("b", "c"), new Pairing<>("c", "b")),
                "themselves");
    }

    @Test
    void rejectsDuplicateRecipient() {
        assertViolation(List.of(new Pairing<>("a", "b"), new Pairing<>("b", "c"), new Pairing<>("c", "b")),
                "receives more than once");
    }

    @Test
    void rejectsDuplicateGiver() {
        assertViolation(List.of(new Pairing<>("a", "b"), new Pairing<>("a", "c"), new Pairing<>("c", "a")),
                "gives more than once");
    }

    @Test
    void rejectsMissingParticipant() {
        assertViolation(List.of(new Pairing<>("a", "b"), new Pairing<>("b", "a")), "count");
    }

    @Test
    void rejectsForeignParticipant() {
        assertViolation(List.of(new Pairing<>("a", "b"), new Pairing<>("b", "x"), new Pairing<>("x", "a")),
                "set differs");
    }

    @Test
    void rejectsEmptySide() {
        assertViolation(List.of(new Pairing<>("a", null), new Pairing<>("b", "c"), new Pairing<>("c", "a")), "empty");
    }

    private static void assertViolation(List<Pairing<String>> pairings, String fragment) {
        assertThatThrownBy(() -> DrawInvariants.verify(PARTICIPANTS, pairings))
                .isInstanceOf(DrawInvariants.DrawInvariantViolation.class)
                .hasMessageContaining(fragment);
    }
}
