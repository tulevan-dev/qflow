package com.qflow.hello.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class HelloCorrelationFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {

        String id = request.getHeader(HEADER);

        if (id == null || !isCanonicalUuid(id)) {
            id = UUID.randomUUID().toString();
        }

        response.setHeader(HEADER, id);
        MDC.put("correlationId", id);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("correlationId");
        }
    }

    private boolean isCanonicalUuid(String value) {
        if (value == null || value.length() != 36) return false;
        try {
            return UUID.fromString(value)
                    .toString().equalsIgnoreCase(value);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
