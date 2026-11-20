package com.qflow.hello.infrastructure.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class HelloCorrelationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HelloCorrelationFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String id = RequestCorrelation.getOrCreateId(request);

        response.setHeader(RequestCorrelation.HEADER, id);
        MDC.put("correlationId", id);

        try {
            chain.doFilter(request, response);
            log.atInfo()
                    .addKeyValue("method", request.getMethod())
                    .addKeyValue("path", request.getRequestURI())
                    .addKeyValue("status", response.getStatus())
                    .log("request completed");
        } finally {
            MDC.remove("correlationId");
        }
    }
}
