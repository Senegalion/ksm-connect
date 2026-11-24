package com.ksmconnect.wishlist;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class TrackingParameters {

    private static final Set<String> EXACT_NAMES = Set.of(
            "fbclid", "gclid", "gbraid", "wbraid", "dclid", "msclkid", "yclid", "igshid", "twclid", "ttclid",
            "mc_cid", "mc_eid", "_hsenc", "_hsmi", "mkt_tok", "srsltid", "$3p");
    private static final List<String> PREFIXES = List.of("utm_", "_branch_", "~");

    private TrackingParameters() {
    }

    static String strip(String url) {
        int fragmentStart = url.indexOf('#');
        String fragment = fragmentStart >= 0 ? url.substring(fragmentStart) : "";
        String withoutFragment = fragmentStart >= 0 ? url.substring(0, fragmentStart) : url;
        int queryStart = withoutFragment.indexOf('?');
        if (queryStart < 0) {
            return url;
        }
        String base = withoutFragment.substring(0, queryStart);
        List<String> kept = new ArrayList<>();
        for (String parameter : withoutFragment.substring(queryStart + 1).split("&")) {
            if (!parameter.isEmpty() && !isTracking(parameter)) {
                kept.add(parameter);
            }
        }
        return base + (kept.isEmpty() ? "" : "?" + String.join("&", kept)) + fragment;
    }

    private static boolean isTracking(String parameter) {
        int separator = parameter.indexOf('=');
        String rawName = separator >= 0 ? parameter.substring(0, separator) : parameter;
        String name;
        try {
            name = URLDecoder.decode(rawName, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException malformed) {
            return false;
        }
        return EXACT_NAMES.contains(name) || PREFIXES.stream().anyMatch(name::startsWith);
    }
}
