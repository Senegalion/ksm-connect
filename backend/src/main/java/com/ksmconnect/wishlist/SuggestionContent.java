package com.ksmconnect.wishlist;

import com.ksmconnect.common.error.BusinessRuleException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

record SuggestionContent(SuggestionType type, String url, String description) {

    static final int MAX_URL_LENGTH = 8192;
    static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    static SuggestionContent of(SuggestionType type, String rawUrl, String rawDescription) {
        if (type == null) {
            throw invalid("SUGGESTION_TYPE_REQUIRED");
        }
        return switch (type) {
            case URL -> {
                requireAbsent(rawDescription);
                yield new SuggestionContent(type, validateUrl(rawUrl), null);
            }
            case DESCRIPTION -> {
                requireAbsent(rawUrl);
                yield new SuggestionContent(type, null, validateDescription(rawDescription));
            }
        };
    }

    static String validateUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw invalid("URL_REQUIRED");
        }
        String candidate = rawUrl.strip();
        if (candidate.length() > MAX_URL_LENGTH || candidate.chars().anyMatch(c -> Character.isWhitespace(c) || Character.isISOControl(c))) {
            throw invalid("INVALID_URL");
        }
        try {
            URI uri = new URI(candidate);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!ALLOWED_SCHEMES.contains(scheme) || uri.getHost() == null || uri.getHost().isBlank()
                    || uri.getRawUserInfo() != null || uri.isOpaque()) {
                throw invalid("INVALID_URL");
            }
            String cleaned = TrackingParameters.strip(candidate);
            new URI(cleaned);
            return cleaned;
        } catch (URISyntaxException exception) {
            throw invalid("INVALID_URL");
        }
    }

    private static String validateDescription(String rawDescription) {
        if (rawDescription == null || rawDescription.isBlank()) {
            throw invalid("DESCRIPTION_REQUIRED");
        }
        String normalized = rawDescription.strip();
        if (normalized.length() > MAX_DESCRIPTION_LENGTH
                || normalized.chars().anyMatch(c -> Character.isISOControl(c) && c != '\n')) {
            throw invalid("INVALID_DESCRIPTION");
        }
        return normalized;
    }

    private static void requireAbsent(String value) {
        if (value != null && !value.isBlank()) {
            throw invalid("SUGGESTION_HAS_BOTH_URL_AND_DESCRIPTION");
        }
    }

    private static BusinessRuleException invalid(String code) {
        return new BusinessRuleException(code, "Invalid gift suggestion: " + code);
    }
}
