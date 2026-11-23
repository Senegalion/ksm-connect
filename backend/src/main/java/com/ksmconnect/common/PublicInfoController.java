package com.ksmconnect.common;

import com.ksmconnect.config.KsmProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class PublicInfoController {

    private final KsmProperties properties;

    PublicInfoController(KsmProperties properties) {
        this.properties = properties;
    }

    record PublicInfo(String privacyNoticeVersion, String defaultTimezone, boolean smsEnabled) {
    }

    @GetMapping("/api/public/info")
    PublicInfo info() {
        return new PublicInfo(properties.privacyNoticeVersion(), properties.defaultTimezone(),
                properties.notification().smsEnabled());
    }
}
