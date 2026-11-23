package com.ksmconnect.notification;

import com.ksmconnect.config.KsmProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "ksm.notification", name = "sms-provider", havingValue = "smsapi")
class SmsApiSmsSender implements SmsSender {

    private final RestClient restClient;
    private final String senderName;

    SmsApiSmsSender(RestClient.Builder builder, KsmProperties properties) {
        KsmProperties.Notification notification = properties.notification();
        if (notification.smsApiToken().isBlank()) {
            throw new IllegalStateException("ksm.notification.sms-api-token is required when SMSAPI is enabled");
        }
        this.senderName = notification.smsSenderName();
        this.restClient = builder
                .baseUrl(notification.smsApiBaseUrl())
                .defaultHeader("Authorization", "Bearer " + notification.smsApiToken())
                .build();
    }

    @Override
    public DeliveryReceipt send(SmsMessage message) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("to", message.to().replace("+", ""));
        form.add("message", message.text());
        form.add("encoding", "utf-8");
        form.add("format", "json");
        form.add("idx", message.idempotencyKey());
        form.add("check_idx", "1");
        if (!senderName.isBlank()) {
            form.add("from", senderName);
        }
        try {
            Map<?, ?> response = restClient.post()
                    .uri("/sms.do")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
            return new DeliveryReceipt(extractMessageId(response));
        } catch (RestClientResponseException exception) {
            throw new DeliveryException("SMS_HTTP_" + exception.getStatusCode().value(), false, exception);
        } catch (RestClientException exception) {
            throw new DeliveryException("SMS_SEND_FAILED", false, exception);
        }
    }

    private static String extractMessageId(Map<?, ?> response) {
        if (response == null) {
            throw new DeliveryException("SMS_EMPTY_RESPONSE", false, null);
        }
        if (response.containsKey("error")) {
            throw new DeliveryException("SMS_PROVIDER_ERROR_" + response.get("error"), false, null);
        }
        if (response.get("list") instanceof List<?> list && !list.isEmpty() && list.getFirst() instanceof Map<?, ?> first) {
            return String.valueOf(first.get("id"));
        }
        throw new DeliveryException("SMS_UNEXPECTED_RESPONSE", false, null);
    }
}
