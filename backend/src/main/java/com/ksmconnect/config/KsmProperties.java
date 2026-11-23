package com.ksmconnect.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "ksm")
public record KsmProperties(
        @NotBlank String frontendBaseUrl,
        @NotBlank String tokenPepper,
        @NotBlank @DefaultValue("2026-11") String privacyNoticeVersion,
        @NotBlank @DefaultValue("Europe/Warsaw") String defaultTimezone,
        @Valid @NotNull @DefaultValue Security security,
        @Valid @NotNull @DefaultValue Auth auth,
        @Valid @NotNull @DefaultValue Notification notification,
        @Valid @NotNull @DefaultValue Scheduling scheduling,
        @Valid @NotNull @DefaultValue Retention retention,
        @Valid @NotNull @DefaultValue Email email
) {

    public record Email(
            @DefaultValue("smtp") String provider,
            @DefaultValue("") String acsEndpoint,
            @DefaultValue("") String acsSenderAddress,
            @DefaultValue("") String managedIdentityClientId
    ) {
    }

    public record Security(
            @DefaultValue("") String clientIpHeader,
            @DefaultValue List<String> corsAllowedOrigins,
            @DefaultValue("true") boolean rateLimitingEnabled
    ) {
    }

    public record Auth(
            @DefaultValue("24h") Duration emailVerificationTtl,
            @DefaultValue("1h") Duration passwordResetTtl,
            @Min(1) @DefaultValue("5") int maxFailedLogins,
            @DefaultValue("15m") Duration lockoutDuration
    ) {
    }

    public record Notification(
            @NotBlank @DefaultValue("KSM Connect <no-reply@localhost>") String mailFrom,
            @DefaultValue("none") String smsProvider,
            @DefaultValue("") String smsApiToken,
            @DefaultValue("https://api.smsapi.pl") String smsApiBaseUrl,
            @DefaultValue("") String smsSenderName,
            @Min(1) @DefaultValue("8") int maxAttempts,
            @DefaultValue("30s") Duration initialBackoff,
            @DefaultValue("2h") Duration maxBackoff,
            @Min(1) @DefaultValue("20") int batchSize,
            @DefaultValue("5m") Duration processingTimeout
    ) {
        public boolean smsEnabled() {
            return !"none".equalsIgnoreCase(smsProvider);
        }
    }

    public record Scheduling(
            @DefaultValue("true") boolean enabled
    ) {
    }

    public record Retention(
            @Min(1) @DefaultValue("90") int daysAfterEvent
    ) {
    }
}
