package com.ksmconnect.auth;

import com.ksmconnect.common.error.BusinessRuleException;

import java.util.Locale;
import java.util.Set;

final class PasswordPolicy {

    static final int MIN_LENGTH = 10;
    static final int MAX_LENGTH = 128;

    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "1234567890", "12345678910", "qwertyuiop", "password123", "haslo12345",
            "qwerty1234", "1qaz2wsx3edc", "zaq12wsxcde", "iloveyou12", "administrator");

    private PasswordPolicy() {
    }

    static void validate(String password, String email) {
        if (password == null || password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            throw new BusinessRuleException("WEAK_PASSWORD", "Password length is outside the allowed range");
        }
        String lowered = password.toLowerCase(Locale.ROOT);
        if (COMMON_PASSWORDS.contains(lowered) || lowered.equals(email) || lowered.chars().distinct().count() < 4) {
            throw new BusinessRuleException("WEAK_PASSWORD", "Password is too easy to guess");
        }
    }
}
