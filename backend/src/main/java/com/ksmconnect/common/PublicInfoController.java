package com.ksmconnect.common;

import com.ksmconnect.config.KsmProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class PublicInfoController {

    private final KsmProperties properties;
    private final String environment;

    PublicInfoController(KsmProperties properties, @Value("${KSM_ENVIRONMENT:local}") String environment) {
        this.properties = properties;
        this.environment = environment;
    }

    record PublicInfo(String privacyNoticeVersion, String defaultTimezone, boolean smsEnabled, String environment) {
    }

    @GetMapping("/api/public/info")
    PublicInfo info() {
        return new PublicInfo(properties.privacyNoticeVersion(), properties.defaultTimezone(),
                properties.notification().smsEnabled(), environment);
    }
}
