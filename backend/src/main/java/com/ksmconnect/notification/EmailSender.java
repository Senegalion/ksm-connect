package com.ksmconnect.notification;

public interface EmailSender {

    DeliveryReceipt send(EmailMessage message);
}
