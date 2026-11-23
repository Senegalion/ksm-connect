package com.ksmconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class KsmConnectApplication {

    public static void main(String[] args) {
        SpringApplication.run(KsmConnectApplication.class, args);
    }
}
