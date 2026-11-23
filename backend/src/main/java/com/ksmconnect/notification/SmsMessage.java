package com.ksmconnect.notification;

public record SmsMessage(String to, String text, String idempotencyKey) {

    @Override
    public String toString() {
        return "SmsMessage[redacted]";
    }
}
