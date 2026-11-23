package com.ksmconnect.auth;

import com.ksmconnect.config.KsmProperties;
import com.ksmconnect.notification.EmailMessage;
import com.ksmconnect.notification.EmailSender;
import com.ksmconnect.notification.MailTemplateRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;
import java.util.UUID;

@Component
class AuthMailer {

    private static final Logger log = LoggerFactory.getLogger(AuthMailer.class);

    private final EmailSender emailSender;
    private final MailTemplateRenderer renderer;
    private final KsmProperties properties;

    AuthMailer(EmailSender emailSender, MailTemplateRenderer renderer, KsmProperties properties) {
        this.emailSender = emailSender;
        this.renderer = renderer;
        this.properties = properties;
    }

    @Async
    @TransactionalEventListener
    void onAuthEmailRequested(AuthEmailRequested request) {
        String link = buildLink(request);
        Map<String, Object> variables = Map.of("firstName", request.firstName(), "actionUrl", link);
        try {
            emailSender.send(new EmailMessage(
                    request.recipientEmail(),
                    request.kind().subject(),
                    renderer.renderHtml(request.kind().template(), variables),
                    renderer.renderText(request.kind().template(), variables),
                    UUID.randomUUID().toString()));
        } catch (RuntimeException exception) {
            log.warn("Failed to send {} email: {}", request.kind(), exception.getClass().getSimpleName());
        }
    }

    private String buildLink(AuthEmailRequested request) {
        String base = properties.frontendBaseUrl() + request.kind().frontendPath();
        return request.rawToken() == null ? base : base + "#token=" + request.rawToken();
    }
}
