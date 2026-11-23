package com.ksmconnect.common.token;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SecureTokenGenerator {

    private static final int URL_TOKEN_BYTES = 32;

    private final SecureRandom random = new SecureRandom();

    public String urlSafeToken() {
        byte[] bytes = new byte[URL_TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String fromAlphabet(String alphabet, int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return builder.toString();
    }
}
