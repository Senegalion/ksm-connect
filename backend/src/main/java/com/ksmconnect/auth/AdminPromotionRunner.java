package com.ksmconnect.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "ksm.tasks", name = "promote-admin")
class AdminPromotionRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminPromotionRunner.class);

    private final AdminPromotionTask task;
    private final ConfigurableApplicationContext context;
    private final String email;

    AdminPromotionRunner(AdminPromotionTask task, ConfigurableApplicationContext context,
                         @Value("${ksm.tasks.promote-admin}") String email) {
        this.task = task;
        this.context = context;
        this.email = email;
    }

    @Override
    public void run(ApplicationArguments args) {
        int exitCode = 0;
        try {
            log.info("Administrator role granted to user {}", task.promote(email).getId());
        } catch (RuntimeException exception) {
            log.error("Administrator promotion failed: {}", exception.getMessage());
            exitCode = 1;
        }
        int code = exitCode;
        System.exit(SpringApplication.exit(context, () -> code));
    }
}
