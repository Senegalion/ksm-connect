package com.ksmconnect.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

@Component
class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final NotificationOutboxRepository repository;
    private final AssignmentDirectory assignmentDirectory;
    private final AssignmentMessageComposer composer;
    private final EmailSender emailSender;
    private final ObjectProvider<SmsSender> smsSender;
    private final RetryPolicy retryPolicy;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    NotificationDispatcher(NotificationOutboxRepository repository, AssignmentDirectory assignmentDirectory,
                           AssignmentMessageComposer composer, EmailSender emailSender,
                           ObjectProvider<SmsSender> smsSender, RetryPolicy retryPolicy,
                           TransactionTemplate transactionTemplate, Clock clock) {
        this.repository = repository;
        this.assignmentDirectory = assignmentDirectory;
        this.composer = composer;
        this.emailSender = emailSender;
        this.smsSender = smsSender;
        this.retryPolicy = retryPolicy;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    void dispatch(UUID outboxId) {
        Optional<NotificationOutbox> claimed = repository.findById(outboxId).filter(NotificationOutbox::isProcessing);
        if (claimed.isEmpty()) {
            return;
        }
        NotificationOutbox entry = claimed.get();
        try {
            DeliveryReceipt receipt = deliver(entry);
            complete(outboxId, receipt);
        } catch (DeliveryException exception) {
            fail(outboxId, exception.getErrorCode(), exception.isPermanent());
        } catch (RuntimeException exception) {
            fail(outboxId, "UNEXPECTED_" + exception.getClass().getSimpleName(), false);
        }
    }

    private DeliveryReceipt deliver(NotificationOutbox entry) {
        AssignmentNotice notice = assignmentDirectory.noticeForGiver(entry.getParticipantId())
                .orElseThrow(() -> new DeliveryException("ASSIGNMENT_NOT_FOUND", true, null));
        return switch (entry.getChannel()) {
            case EMAIL -> emailSender.send(composer.email(notice, entry.getId().toString()));
            case SMS -> sendSms(notice, entry);
        };
    }

    private DeliveryReceipt sendSms(AssignmentNotice notice, NotificationOutbox entry) {
        SmsSender sender = smsSender.getIfAvailable();
        if (sender == null) {
            throw new DeliveryException("SMS_PROVIDER_DISABLED", true, null);
        }
        if (notice.giverPhoneNumber() == null) {
            throw new DeliveryException("PHONE_NUMBER_MISSING", true, null);
        }
        return sender.send(composer.sms(notice, entry.getId().toString()));
    }

    private void complete(UUID outboxId, DeliveryReceipt receipt) {
        transactionTemplate.executeWithoutResult(status -> repository.findById(outboxId)
                .ifPresent(entry -> entry.markSent(clock.instant(), receipt.providerMessageId())));
        log.info("Notification {} delivered", outboxId);
    }

    private void fail(UUID outboxId, String errorCode, boolean permanent) {
        transactionTemplate.executeWithoutResult(status -> repository.findById(outboxId).ifPresent(entry -> {
            entry.markAttemptFailed(errorCode, permanent,
                    retryPolicy.nextAttemptAt(clock.instant(), entry.getAttemptCount()));
            log.warn("Notification {} attempt {} failed with {} -> {}", outboxId, entry.getAttemptCount(),
                    errorCode, entry.getStatus());
        }));
    }
}
