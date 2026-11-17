package com.qflow.gateway.config;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationFilter implements WebFilter {

    private static final String HEADER = "X-Correlation-Id";
    private static final Logger log =
            LoggerFactory.getLogger(CorrelationFilter.class);

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            WebFilterChain chain) {

        String incoming = exchange.getRequest()
                .getHeaders().getFirst(HEADER);

        String id = valid(incoming)
                ? incoming
                : UUID.randomUUID().toString();

        // từ req tạo 1 bản sao mới để tùy chỉnh
        var request = exchange.getRequest().mutate()
                .headers(h -> h.set(HEADER, id))
                .build();

        // tạo exchange mới sử dụng request vừa cập nhật
        var updated = exchange.mutate()
                .request(request).build();

        updated.getResponse().beforeCommit(() -> {
            updated.getResponse().getHeaders().set(HEADER, id);
            return Mono.empty();
        });

        return chain.filter(updated)
                .doFinally(signal -> log.atInfo()
                        .addKeyValue("correlationId", id)
                        .addKeyValue("method",
                                request.getMethod().name())
                        .addKeyValue("path",
                                request.getPath().value())
                        .log("request completed"))
                .contextWrite(ctx -> ctx.put("correlationId", id));
    }

    private boolean valid(String value) {
        if (value == null || value.length() != 36) return false;
        try {
            return UUID.fromString(value)
                    .toString().equalsIgnoreCase(value);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
