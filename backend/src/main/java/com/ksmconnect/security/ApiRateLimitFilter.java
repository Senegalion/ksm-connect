package com.ksmconnect.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class ApiRateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;
    private final ClientIpResolver clientIpResolver;

    public ApiRateLimitFilter(RateLimiter rateLimiter, ClientIpResolver clientIpResolver) {
        this.rateLimiter = rateLimiter;
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!rateLimiter.tryConsume(RateLimitPolicy.API_PER_IP, clientIpResolver.resolve(request))) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write("{\"status\":429,\"title\":\"Too Many Requests\",\"code\":\"RATE_LIMITED\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
