package com.ksmconnect.notification;

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.EmailClientBuilder;
import com.azure.communication.email.models.EmailSendResult;
import com.azure.communication.email.models.EmailSendStatus;
import com.azure.core.util.polling.LongRunningOperationStatus;
import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.SyncPoller;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.ksmconnect.config.KsmProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConditionalOnProperty(prefix = "ksm.email", name = "provider", havingValue = "acs")
class AcsEmailSender implements EmailSender {

    private static final Duration SEND_TIMEOUT = Duration.ofSeconds(60);

    private final EmailClient client;
    private final String senderAddress;

    AcsEmailSender(KsmProperties properties) {
        KsmProperties.Email email = properties.email();
        if (email.acsEndpoint().isBlank() || email.acsSenderAddress().isBlank()) {
            throw new IllegalStateException("ksm.email.acs-endpoint and ksm.email.acs-sender-address are required for ACS");
        }
        DefaultAzureCredentialBuilder credential = new DefaultAzureCredentialBuilder();
        if (!email.managedIdentityClientId().isBlank()) {
            credential.managedIdentityClientId(email.managedIdentityClientId());
        }
        this.client = new EmailClientBuilder()
                .endpoint(email.acsEndpoint())
                .credential(credential.build())
                .buildClient();
        this.senderAddress = email.acsSenderAddress();
    }

    @Override
    public DeliveryReceipt send(EmailMessage message) {
        com.azure.communication.email.models.EmailMessage acsMessage = new com.azure.communication.email.models.EmailMessage()
                .setSenderAddress(senderAddress)
                .setToRecipients(message.to())
                .setSubject(message.subject())
                .setBodyHtml(message.htmlBody())
                .setBodyPlainText(message.textBody());
        try {
            SyncPoller<EmailSendResult, EmailSendResult> poller = client.beginSend(acsMessage);
            PollResponse<EmailSendResult> response = poller.waitForCompletion(SEND_TIMEOUT);
            if (response.getStatus() != LongRunningOperationStatus.SUCCESSFULLY_COMPLETED) {
                throw new DeliveryException("EMAIL_ACS_" + response.getStatus(), false, null);
            }
            EmailSendResult result = response.getValue();
            if (result == null || result.getStatus() != EmailSendStatus.SUCCEEDED) {
                throw new DeliveryException("EMAIL_ACS_NOT_SUCCEEDED", false, null);
            }
            return new DeliveryReceipt(result.getId());
        } catch (DeliveryException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new DeliveryException("EMAIL_ACS_SEND_FAILED", false, exception);
        }
    }
}
