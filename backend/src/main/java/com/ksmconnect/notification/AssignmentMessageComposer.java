package com.ksmconnect.notification;

import com.ksmconnect.config.KsmProperties;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Component
class AssignmentMessageComposer {

    static final String EMAIL_SUBJECT = "🎁 Twoje losowanie na Wigilię KSM – KSM Connect";
    private static final String TEMPLATE = "assignment-ready";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale.of("pl", "PL"));

    private final MailTemplateRenderer renderer;
    private final String frontendBaseUrl;

    AssignmentMessageComposer(MailTemplateRenderer renderer, KsmProperties properties) {
        this.renderer = renderer;
        this.frontendBaseUrl = properties.frontendBaseUrl();
    }

    EmailMessage email(AssignmentNotice notice, String idempotencyKey) {
        Map<String, Object> variables = variables(notice);
        return new EmailMessage(notice.giverEmail(), EMAIL_SUBJECT,
                renderer.renderHtml(TEMPLATE, variables), renderer.renderText(TEMPLATE, variables), idempotencyKey);
    }

    SmsMessage sms(AssignmentNotice notice, String idempotencyKey) {
        String text = """
                🎁 KSM Connect – losowanie zakończone!

                Na Wigilii KSM przygotowujesz prezent dla: %s.

                Szczegóły oraz lista prezentów: %s

                Powodzenia i udanej zabawy! 🎄""".formatted(notice.recipientFirstName(), assignmentUrl(notice));
        return new SmsMessage(notice.giverPhoneNumber(), text, idempotencyKey);
    }

    String assignmentUrl(AssignmentNotice notice) {
        return frontendBaseUrl + "/events/" + notice.eventId() + "/assignment";
    }

    private Map<String, Object> variables(AssignmentNotice notice) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("giverFirstName", notice.giverFirstName());
        variables.put("recipientFullName", notice.recipientFullName());
        variables.put("eventName", notice.eventName());
        variables.put("eventLocation", notice.eventLocation());
        variables.put("eventDate", notice.eventStartsAt() == null ? null
                : DATE_FORMAT.format(notice.eventStartsAt().atZone(ZoneId.of(notice.eventTimezone()))));
        variables.put("assignmentUrl", assignmentUrl(notice));
        return variables;
    }
}
