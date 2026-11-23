package com.ksmconnect.common.token;

import com.ksmconnect.config.KsmProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

@Component
public class TokenHasher {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec key;

    public TokenHasher(KsmProperties properties) {
        byte[] pepper = properties.tokenPepper().getBytes(StandardCharsets.UTF_8);
        if (pepper.length < 32) {
            throw new IllegalStateException("ksm.token-pepper must be at least 32 bytes long");
        }
        this.key = new SecretKeySpec(pepper, ALGORITHM);
    }

    public String hash(String rawToken) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            return HexFormat.of().formatHex(mac.doFinal(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Token hashing unavailable", exception);
        }
    }
}
