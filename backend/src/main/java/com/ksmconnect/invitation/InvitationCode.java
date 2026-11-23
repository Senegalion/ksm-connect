package com.ksmconnect.invitation;

import java.util.Locale;
import java.util.Optional;

final class InvitationCode {

    static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    static final int LENGTH = 10;
    private static final String PREFIX = "KSM";

    private InvitationCode() {
    }

    static String format(String canonical) {
        return PREFIX + "-" + canonical.substring(0, LENGTH / 2) + "-" + canonical.substring(LENGTH / 2);
    }

    static Optional<String> canonicalize(String input) {
        if (input == null || input.length() > 64) {
            return Optional.empty();
        }
        String compact = input.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        if (compact.length() == PREFIX.length() + LENGTH && compact.startsWith(PREFIX)) {
            compact = compact.substring(PREFIX.length());
        }
        if (compact.length() != LENGTH || !compact.chars().allMatch(c -> ALPHABET.indexOf(c) >= 0)) {
            return Optional.empty();
        }
        return Optional.of(compact);
    }
}
