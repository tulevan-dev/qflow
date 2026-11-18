package com.qflow.hello.infrastructure.observability;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

public final class RequestCorrelation {
    public static final String HEADER = "X-Correlation-Id";
    private static final String ATTRIBUTE = RequestCorrelation.class.getName() + ".id";

    private RequestCorrelation() {}

    public static String getOrCreateId(HttpServletRequest request) {
        String id = (String) request.getAttribute(ATTRIBUTE);
        if (id == null) {
            String incoming = request.getHeader(HEADER);
            id = isCanonicalUuid(incoming) ? incoming : UUID.randomUUID().toString();
            request.setAttribute(ATTRIBUTE, id);
        }
        return id;
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
