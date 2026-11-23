package com.ksmconnect.auth;

import com.ksmconnect.common.error.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {"short", "1234567890", "aaaaaaaaaaaa", "ababababab", "Password123"})
    void rejectsWeakPasswords(String password) {
        assertThatThrownBy(() -> PasswordPolicy.validate(password, "anna@example.com"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsPasswordEqualToEmail() {
        assertThatThrownBy(() -> PasswordPolicy.validate("anna@example.com", "anna@example.com"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsOverlongPassword() {
        assertThatThrownBy(() -> PasswordPolicy.validate("ab1!".repeat(40), "anna@example.com"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void acceptsLongPassphrase() {
        assertThatCode(() -> PasswordPolicy.validate("zimowa wigilia przy choince", "anna@example.com"))
                .doesNotThrowAnyException();
    }
}
