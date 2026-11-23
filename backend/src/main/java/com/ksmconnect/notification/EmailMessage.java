package com.ksmconnect.notification;

public record EmailMessage(String to, String subject, String htmlBody, String textBody, String idempotencyKey) {

    @Override
    public String toString() {
        return "EmailMessage[redacted]";
    }
}
