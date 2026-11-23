package com.ksmconnect.draw;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DrawInvariants {

    private DrawInvariants() {
    }

    public static <T> void verify(List<T> participants, List<Pairing<T>> pairings) {
        Set<T> expected = new HashSet<>(participants);
        require(expected.size() == participants.size(), "participants contain duplicates");
        require(expected.size() >= 2, "at least two participants are required");
        require(pairings.size() == expected.size(), "assignment count differs from participant count");

        Set<T> givers = new HashSet<>();
        Set<T> recipients = new HashSet<>();
        for (Pairing<T> pairing : pairings) {
            require(pairing.giver() != null && pairing.recipient() != null, "pairing contains an empty side");
            require(!pairing.giver().equals(pairing.recipient()), "participant assigned to themselves");
            require(givers.add(pairing.giver()), "participant gives more than once");
            require(recipients.add(pairing.recipient()), "participant receives more than once");
        }
        require(givers.equals(expected), "giver set differs from participant set");
        require(recipients.equals(expected), "recipient set differs from participant set");
    }

    private static void require(boolean condition, String violation) {
        if (!condition) {
            throw new DrawInvariantViolation(violation);
        }
    }

    public static final class DrawInvariantViolation extends IllegalStateException {
        DrawInvariantViolation(String violation) {
            super("Draw invariant violated: " + violation);
        }
    }
}
