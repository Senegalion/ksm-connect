package com.ksmconnect.security;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserSessionService {

    private final JdbcClient jdbcClient;

    public UserSessionService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Transactional
    public int terminateAllSessions(UUID userId) {
        return jdbcClient.sql("DELETE FROM spring_session WHERE principal_name = :principal")
                .param("principal", userId.toString())
                .update();
    }
}
