package com.qflow.gateway.observability;

import java.util.UUID;
import org.springframework.web.server.ServerWebExchange;

public final class RequestCorrelation {
    public static final String HEADER = "X-Correlation-Id";
    private static final String ATTRIBUTE = RequestCorrelation.class.getName() + ".id";

    private RequestCorrelation() {}

    public static String getOrCreateId(ServerWebExchange exchange) {
        var attributes = exchange.getAttributes();
        return (String)
                attributes.computeIfAbsent(
                        ATTRIBUTE,
                        key -> {
                            String incoming = exchange.getRequest().getHeaders().getFirst(HEADER);
                            return isCanonicalUuid(incoming)
                                    ? incoming
                                    : UUID.randomUUID().toString();
                        });
    }

    private static boolean isCanonicalUuid(String value) {
        if (value == null || value.length() != 36) {
            return false;
        }
        try {
            return UUID.fromString(value).toString().equalsIgnoreCase(value);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
