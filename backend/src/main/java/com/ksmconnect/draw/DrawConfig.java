package com.ksmconnect.draw;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class DrawConfig {

    @Bean
    DerangementGenerator derangementGenerator() {
        return new DerangementGenerator();
    }
}
