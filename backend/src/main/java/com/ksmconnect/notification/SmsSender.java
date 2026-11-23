package com.ksmconnect.notification;

public interface SmsSender {

    DeliveryReceipt send(SmsMessage message);
}
