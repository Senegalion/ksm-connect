package com.ksmconnect.notification;

import com.ksmconnect.config.KsmProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnProperty(prefix = "ksm.email", name = "provider", havingValue = "smtp", matchIfMissing = true)
class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final String from;

    SmtpEmailSender(JavaMailSender mailSender, KsmProperties properties) {
        this.mailSender = mailSender;
        this.from = properties.notification().mailFrom();
    }

    @Override
    public DeliveryReceipt send(EmailMessage message) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            helper.setText(message.textBody(), message.htmlBody());
            mimeMessage.setHeader("X-KSM-Idempotency-Key", message.idempotencyKey());
            mailSender.send(mimeMessage);
            return new DeliveryReceipt(mimeMessage.getMessageID());
        } catch (MailParseException | MailPreparationException exception) {
            throw new DeliveryException("EMAIL_INVALID_MESSAGE", true, exception);
        } catch (MailAuthenticationException exception) {
            throw new DeliveryException("EMAIL_AUTHENTICATION_FAILED", false, exception);
        } catch (MailException exception) {
            throw new DeliveryException("EMAIL_SEND_FAILED", false, exception);
        } catch (MessagingException exception) {
            throw new DeliveryException("EMAIL_INVALID_MESSAGE", true, exception);
        }
    }
}
