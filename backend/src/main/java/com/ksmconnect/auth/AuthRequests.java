package com.ksmconnect.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

final class AuthRequests {

    private AuthRequests() {
    }

    record Register(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH) String password,
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @NotBlank @Size(max = 30) String phoneNumber,
            @AssertTrue boolean privacyNoticeAccepted
    ) {
        @Override
        public String toString() {
            return "Register[redacted]";
        }
    }

    record Login(@NotBlank @Size(max = 254) String email, @NotBlank @Size(max = 256) String password) {
        @Override
        public String toString() {
            return "Login[redacted]";
        }
    }

    record TokenConfirmation(@NotBlank @Size(max = 128) String token) {
    }

    record EmailOnly(@NotBlank @Email @Size(max = 254) String email) {
    }

    record PasswordResetConfirmation(
            @NotBlank @Size(max = 128) String token,
            @NotBlank @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH) String newPassword
    ) {
        @Override
        public String toString() {
            return "PasswordResetConfirmation[redacted]";
        }
    }

    record PasswordChange(
            @NotBlank @Size(max = 256) String currentPassword,
            @NotBlank @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH) String newPassword
    ) {
        @Override
        public String toString() {
            return "PasswordChange[redacted]";
        }
    }
}
