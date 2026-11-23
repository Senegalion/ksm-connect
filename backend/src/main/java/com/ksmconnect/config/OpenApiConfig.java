package com.ksmconnect.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI ksmConnectOpenApi() {
        return new OpenAPI().info(new Info()
                .title("KSM Connect API")
                .version("v1")
                .description("Event management, invitations, wishlists and Secret Santa drawings for KSM communities."));
    }
}
