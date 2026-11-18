package com.qflow.gateway.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(CorrelationFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        String id = RequestCorrelation.getOrCreateId(exchange);

        // từ req tạo 1 bản sao mới để tùy chỉnh
        var request =
                exchange.getRequest()
                        .mutate()
                        .headers(h -> h.set(RequestCorrelation.HEADER, id))
                        .build();

        // tạo exchange mới sử dụng request vừa cập nhật
        var updated = exchange.mutate().request(request).build();

        updated.getResponse()
                .beforeCommit(() -> {
                    updated.getResponse().getHeaders().set(RequestCorrelation.HEADER, id);
                    return Mono.empty();
                });

        return chain.filter(updated)
                .doFinally(signal ->
                        log.atInfo()
                                .addKeyValue("correlationId", id)
                                .addKeyValue("method", request.getMethod().name())
                                .addKeyValue("path", request.getPath().value())
                                .log("request completed"))
                .contextWrite(ctx -> ctx.put("correlationId", id));
    }
}
