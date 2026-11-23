package com.ksmconnect.scheduler;

import com.ksmconnect.privacy.PersonalDataRetentionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class RetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(RetentionScheduler.class);

    private final PersonalDataRetentionService retentionService;

    RetentionScheduler(PersonalDataRetentionService retentionService) {
        this.retentionService = retentionService;
    }

    @Scheduled(fixedDelayString = "${ksm.scheduling.retention-interval:PT6H}", initialDelayString = "PT2M")
    void tick() {
        try {
            retentionService.eventsDueForPurge().forEach(retentionService::purgeEvent);
            int removedAccounts = retentionService.purgeStaleAccountsAndTokens();
            if (removedAccounts > 0) {
                log.info("Removed {} unverified accounts past retention", removedAccounts);
            }
        } catch (RuntimeException exception) {
            log.error("Retention job failed: {}", exception.getClass().getSimpleName(), exception);
        }
    }
}
