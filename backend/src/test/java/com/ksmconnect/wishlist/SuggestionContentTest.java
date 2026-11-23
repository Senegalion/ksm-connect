package com.ksmconnect.wishlist;

import com.ksmconnect.common.error.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SuggestionContentTest {

    @ParameterizedTest
    @ValueSource(strings = {"https://example.com/book", "http://sklep.pl/produkt?id=12&kolor=niebieski",
            "HTTPS://EXAMPLE.COM/A", "https://www.empik.com/ksiazka,p123,ksiazka-p#opis"})
    void acceptsHttpAndHttpsLinks(String url) {
        SuggestionContent content = SuggestionContent.of(SuggestionType.URL, url, null);
        assertThat(content.url()).isEqualTo(url);
        assertThat(content.description()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "JavaScript:alert(document.cookie)", "data:text/html,<script>x</script>",
            "file:///etc/passwd", "ftp://example.com/file", "vbscript:msgbox", "//example.com/no-scheme",
            "example.com", "https://", "https://user:pass@example.com", "https://exa mple.com",
            "https://example.com/\nheader", "mailto:someone@example.com", "http:/missing-slash"})
    void rejectsUnsafeOrMalformedLinks(String url) {
        assertThatThrownBy(() -> SuggestionContent.of(SuggestionType.URL, url, null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsOverlongLinks() {
        String url = "https://example.com/" + "a".repeat(SuggestionContent.MAX_URL_LENGTH);
        assertCode(() -> SuggestionContent.of(SuggestionType.URL, url, null), "INVALID_URL");
    }

    @Test
    void trimsDescription() {
        assertThat(SuggestionContent.of(SuggestionType.DESCRIPTION, null, "  Ciepły szalik  ").description())
                .isEqualTo("Ciepły szalik");
    }

    @Test
    void storesDescriptionVerbatimForSafeRendering() {
        String markup = "<img src=x onerror=alert(1)> gra planszowa";
        assertThat(SuggestionContent.of(SuggestionType.DESCRIPTION, null, markup).description()).isEqualTo(markup);
    }

    @Test
    void rejectsBothUrlAndDescription() {
        assertCode(() -> SuggestionContent.of(SuggestionType.URL, "https://example.com", "opis"),
                "SUGGESTION_HAS_BOTH_URL_AND_DESCRIPTION");
        assertCode(() -> SuggestionContent.of(SuggestionType.DESCRIPTION, "https://example.com", "opis"),
                "SUGGESTION_HAS_BOTH_URL_AND_DESCRIPTION");
    }

    @Test
    void requiresContentMatchingType() {
        assertCode(() -> SuggestionContent.of(SuggestionType.URL, " ", null), "URL_REQUIRED");
        assertCode(() -> SuggestionContent.of(SuggestionType.DESCRIPTION, null, "   "), "DESCRIPTION_REQUIRED");
        assertCode(() -> SuggestionContent.of(null, "https://example.com", null), "SUGGESTION_TYPE_REQUIRED");
    }

    @Test
    void rejectsOverlongOrControlCharacterDescriptions() {
        assertCode(() -> SuggestionContent.of(SuggestionType.DESCRIPTION, null, "a".repeat(501)), "INVALID_DESCRIPTION");
        assertCode(() -> SuggestionContent.of(SuggestionType.DESCRIPTION, null, "abc\u0000def"), "INVALID_DESCRIPTION");
        assertThat(SuggestionContent.of(SuggestionType.DESCRIPTION, null, "linia 1\nlinia 2").description())
                .contains("\n");
    }

    private static void assertCode(Runnable action, String code) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessRuleException.class)
                .extracting(exception -> ((BusinessRuleException) exception).getCode())
                .isEqualTo(code);
    }
}
