package com.ksmconnect.security;

import com.ksmconnect.config.KsmProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    @Test
    void forwardedForUsesHopAppendedByTrustedProxy() {
        MockHttpServletRequest request = request("X-Forwarded-For", "6.6.6.6, 203.0.113.7");
        assertThat(resolver("X-Forwarded-For").resolve(request)).isEqualTo("203.0.113.7");
    }

    @Test
    void cloudflareHeaderIsUsedVerbatim() {
        MockHttpServletRequest request = request("CF-Connecting-IP", "198.51.100.4");
        assertThat(resolver("CF-Connecting-IP").resolve(request)).isEqualTo("198.51.100.4");
    }

    @Test
    void untrustedHeadersAreIgnoredWithoutConfiguration() {
        MockHttpServletRequest request = request("CF-Connecting-IP", "198.51.100.4");
        request.setRemoteAddr("10.0.0.9");
        assertThat(resolver("").resolve(request)).isEqualTo("10.0.0.9");
    }

    @Test
    void fallsBackToRemoteAddressWhenHeaderMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        assertThat(resolver("X-Forwarded-For").resolve(request)).isEqualTo("10.0.0.5");
    }

    private static MockHttpServletRequest request(String header, String value) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(header, value);
        return request;
    }

    private static ClientIpResolver resolver(String header) {
        return new ClientIpResolver(new KsmProperties("http://localhost", "x".repeat(32), "v1", "Europe/Warsaw",
                new KsmProperties.Security(header, List.of(), true),
                new KsmProperties.Auth(Duration.ofHours(24), Duration.ofHours(1), 5, Duration.ofMinutes(15)),
                new KsmProperties.Notification("KSM <a@b.c>", "none", "", "https://api.smsapi.pl", "", 8,
                        Duration.ofSeconds(30), Duration.ofHours(2), 20, Duration.ofMinutes(5)),
                new KsmProperties.Scheduling(false),
                new KsmProperties.Retention(90),
                new KsmProperties.Email("smtp", "", "", "")));
    }
}
