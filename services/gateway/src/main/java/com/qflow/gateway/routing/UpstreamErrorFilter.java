package com.qflow.gateway.routing;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CLIENT_RESPONSE_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CLIENT_RESPONSE_HEADER_NAMES;

import java.util.List;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.NettyWriteResponseFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class UpstreamErrorFilter implements GlobalFilter, Ordered {
    private static final List<String> ERROR_HEADERS =
            List.of(HttpHeaders.ALLOW, HttpHeaders.RETRY_AFTER, HttpHeaders.WWW_AUTHENTICATE);

    @Override
    public int getOrder() {
        return NettyWriteResponseFilter.WRITE_RESPONSE_FILTER_ORDER + 1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Run after routing sets the upstream status and before the response writer commits the
        // body.
        return chain.filter(exchange)
                .then(
                        Mono.defer(() -> {
                            var response = exchange.getResponse();
                            var status = response.getStatusCode();
                            var headers = response.getHeaders();
                            if (response.isCommitted()
                                    || status == null
                                    || !status.isError()
                                    || exchange.getAttribute(CLIENT_RESPONSE_ATTR) == null
                                    || (status.is4xxClientError() && hasProblemBody(headers))) {
                                return Mono.empty();
                            }
                            ErrorResponseException error = new ErrorResponseException(status);
                            for (String name : ERROR_HEADERS) {
                                List<String> values = headers.get(name);
                                if (values != null) {
                                    error.getHeaders().put(name, List.copyOf(values));
                                }
                            }
                            // The original headers describe the upstream body. The response writer
                            // disposes its connection on error.
                            var upstreamHeaders =
                                    exchange.<Iterable<String>>getAttribute(
                                            CLIENT_RESPONSE_HEADER_NAMES);
                            if (upstreamHeaders != null) {
                                upstreamHeaders.forEach(headers::remove);
                            }
                            return Mono.error(error);
                        }));
    }

    private boolean hasProblemBody(HttpHeaders headers) {
        try {
            return MediaType.APPLICATION_PROBLEM_JSON.equalsTypeAndSubtype(
                    headers.getContentType());
        } catch (InvalidMediaTypeException ex) {
            return false;
        }
    }
}
