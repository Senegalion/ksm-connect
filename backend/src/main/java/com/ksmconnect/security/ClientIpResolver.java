package com.ksmconnect.security;

import com.ksmconnect.config.KsmProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {

    private final String trustedHeader;

    public ClientIpResolver(KsmProperties properties) {
        this.trustedHeader = properties.security().clientIpHeader();
    }

    public String resolve(HttpServletRequest request) {
        if (!trustedHeader.isBlank()) {
            String headerValue = request.getHeader(trustedHeader);
            if (headerValue != null && !headerValue.isBlank()) {
                return headerValue.split(",")[0].strip();
            }
        }
        return request.getRemoteAddr();
    }
}
