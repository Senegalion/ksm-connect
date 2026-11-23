package com.ksmconnect.support;

import com.ksmconnect.notification.DeliveryException;
import com.ksmconnect.notification.DeliveryReceipt;
import com.ksmconnect.notification.EmailMessage;
import com.ksmconnect.notification.EmailSender;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RecordingEmailSender implements EmailSender {

    private static final Pattern TOKEN = Pattern.compile("#token=([A-Za-z0-9_-]+)");

    private final List<EmailMessage> sent = new CopyOnWriteArrayList<>();
    private final AtomicInteger failuresRemaining = new AtomicInteger();

    @Override
    public DeliveryReceipt send(EmailMessage message) {
        if (failuresRemaining.getAndUpdate(value -> Math.max(0, value - 1)) > 0) {
            throw new DeliveryException("EMAIL_SEND_FAILED", false, null);
        }
        sent.add(message);
        return new DeliveryReceipt("<" + UUID.randomUUID() + "@test>");
    }

    public void failNext(int count) {
        failuresRemaining.set(count);
    }

    public void reset() {
        sent.clear();
        failuresRemaining.set(0);
    }

    public List<EmailMessage> sentTo(String email) {
        return sent.stream().filter(message -> message.to().equals(email)).toList();
    }

    public List<EmailMessage> all() {
        return List.copyOf(sent);
    }

    public String lastTokenFor(String email, String subjectFragment) {
        List<EmailMessage> messages = sentTo(email).stream()
                .filter(message -> message.subject().contains(subjectFragment))
                .toList();
        if (messages.isEmpty()) {
            throw new AssertionError("No email matching subject fragment was sent");
        }
        Matcher matcher = TOKEN.matcher(messages.getLast().textBody());
        if (!matcher.find()) {
            throw new AssertionError("Email does not contain a token link");
        }
        return matcher.group(1);
    }
}
