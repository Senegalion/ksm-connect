package com.ksmconnect.invitation;

import com.ksmconnect.common.token.SecureTokenGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InvitationCodeTest {

    private final SecureTokenGenerator generator = new SecureTokenGenerator();

    @Test
    void formatsWithPrefixAndGroups() {
        assertThat(InvitationCode.format("ABCDEFGHJK")).isEqualTo("KSM-ABCDE-FGHJK");
    }

    @ParameterizedTest
    @ValueSource(strings = {"KSM-ABCDE-FGHJK", "ksm-abcde-fghjk", "ABCDEFGHJK", " abcde fghjk ", "KSM ABCDE FGHJK", "ksmabcdefghjk"})
    void acceptsCommonInputVariants(String input) {
        assertThat(InvitationCode.canonicalize(input)).contains("ABCDEFGHJK");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "KSM-ABCDE", "ABCDEFGHJKL", "ABCDEFGHI0", "ABCDEFGHJ1", "ABCDEFGHJO",
            "<script>alert(1)</script>", "' OR 1=1 --"})
    void rejectsMalformedCodes(String input) {
        assertThat(InvitationCode.canonicalize(input)).isEmpty();
    }

    @Test
    void rejectsOverlongInputWithoutProcessing() {
        assertThat(InvitationCode.canonicalize("A".repeat(65))).isEmpty();
    }

    @Test
    void alphabetExcludesAmbiguousCharacters() {
        assertThat(InvitationCode.ALPHABET).doesNotContain("0", "O", "1", "I", "L");
    }

    @Test
    void generatedCodesUseAlphabetAndAreUnique() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 20_000; i++) {
            String code = generator.fromAlphabet(InvitationCode.ALPHABET, InvitationCode.LENGTH);
            assertThat(InvitationCode.canonicalize(code)).contains(code);
            codes.add(code);
        }
        assertThat(codes).hasSize(20_000);
    }

    @Test
    void keyspaceProvidesAtLeastFortyEightBitsOfEntropy() {
        double bits = InvitationCode.LENGTH * (Math.log(InvitationCode.ALPHABET.length()) / Math.log(2));
        assertThat(bits).isGreaterThan(48);
    }
}
