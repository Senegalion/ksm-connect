package com.ksmconnect.auth;

import com.ksmconnect.support.IntegrationTest;
import com.ksmconnect.support.TestApi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class AdminPromotionTaskIT {

    @Autowired
    TestApi api;
    @Autowired
    AdminPromotionTask task;

    @Test
    void promotionGrantsAdminAndInvalidatesExistingSessions() throws Exception {
        TestApi.Session before = api.participant("Awans");
        api.get(before, "/api/admin/events").andExpect(status().isForbidden());

        task.promote(before.email());

        api.get(before, "/api/admin/events").andExpect(status().isUnauthorized());
        TestApi.Session after = api.login(before.email());
        api.get(after, "/api/admin/events").andExpect(status().isOk());
        api.get(after, "/api/auth/session").andExpect(jsonPath("$.user.roles.length()").value(2));
    }

    @Test
    void promotionRequiresExistingVerifiedAccount() throws Exception {
        assertThatThrownBy(() -> task.promote(TestApi.uniqueEmail("missing"))).isInstanceOf(IllegalArgumentException.class);
        String email = TestApi.uniqueEmail("unverified");
        api.register(email, "Nie", "Potwierdzony");
        assertThatThrownBy(() -> task.promote(email)).isInstanceOf(IllegalStateException.class);
    }
}
