package com.ksmconnect.user;

import com.ksmconnect.common.error.BusinessRuleException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneNumberNormalizerTest {

    private final PhoneNumberNormalizer normalizer = new PhoneNumberNormalizer();

    @ParameterizedTest
    @CsvSource({"600 100 200, +48600100200", "+48 600-100-200, +48600100200", "0048600100200, +48600100200",
            "+49 1512 3456789, +4915123456789"})
    void normalizesToE164(String input, String expected) {
        assertThat(normalizer.toE164(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "abc", "+48 000", "600100200600100200"})
    void rejectsInvalidNumbers(String input) {
        assertThatThrownBy(() -> normalizer.toE164(input)).isInstanceOf(BusinessRuleException.class);
    }
}
