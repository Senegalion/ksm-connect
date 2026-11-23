package com.ksmconnect.support;

import com.ksmconnect.notification.DeliveryReceipt;
import com.ksmconnect.notification.SmsMessage;
import com.ksmconnect.notification.SmsSender;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class RecordingSmsSender implements SmsSender {

    private final List<SmsMessage> sent = new CopyOnWriteArrayList<>();

    @Override
    public DeliveryReceipt send(SmsMessage message) {
        sent.add(message);
        return new DeliveryReceipt(UUID.randomUUID().toString());
    }

    public List<SmsMessage> all() {
        return List.copyOf(sent);
    }

    public void reset() {
        sent.clear();
    }
}
