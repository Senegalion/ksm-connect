package com.ksmconnect.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;

final class ProblemResponseWriter {

    private ProblemResponseWriter() {
    }

    static void write(HttpServletResponse response, HttpStatus status, String code) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("{\"status\":%d,\"title\":\"%s\",\"code\":\"%s\"}"
                .formatted(status.value(), status.getReasonPhrase(), code));
    }
}
