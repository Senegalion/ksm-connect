package com.ksmconnect.security;

import com.ksmconnect.config.KsmProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {

    private static final String FORWARDED_FOR = "X-Forwarded-For";

    private final String trustedHeader;

    public ClientIpResolver(KsmProperties properties) {
        this.trustedHeader = properties.security().clientIpHeader();
    }

    public String resolve(HttpServletRequest request) {
        if (!trustedHeader.isBlank()) {
            String headerValue = request.getHeader(trustedHeader);
            if (headerValue != null && !headerValue.isBlank()) {
                String[] hops = headerValue.split(",");
                String candidate = FORWARDED_FOR.equalsIgnoreCase(trustedHeader) ? hops[hops.length - 1] : hops[0];
                return candidate.strip();
            }
        }
        return request.getRemoteAddr();
    }
}
