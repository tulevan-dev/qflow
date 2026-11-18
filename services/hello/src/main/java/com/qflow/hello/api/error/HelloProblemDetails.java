package com.qflow.hello.api.error;

import com.qflow.hello.infrastructure.observability.RequestCorrelation;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.util.UriUtils;

final class HelloProblemDetails {
    private HelloProblemDetails() {}

    static ProblemDetail create(HttpStatusCode status, String path, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        HttpStatus knownStatus = HttpStatus.resolve(status.value());
        String type =
                knownStatus != null
                        ? knownStatus.name().toLowerCase(Locale.ROOT).replace('_', '-')
                        : "http-" + status.value();
        problem.setType(URI.create("https://qflow.local/problems/" + type));
        if (knownStatus == null) {
            problem.setTitle("HTTP " + status.value());
        }
        problem.setDetail(
                switch (status.value()) {
                    case 400 -> "The request is invalid.";
                    case 404 -> "The requested resource was not found.";
                    case 405 -> "The HTTP method is not supported for this resource.";
                    case 415 -> "The request media type is not supported.";
                    default ->
                            status.is5xxServerError()
                                    ? "The server could not complete the request."
                                    : "The request could not be completed.";
                });
        if (path.startsWith("//")) {
            path = "/%2F" + path.substring(2);
        }
        try {
            problem.setInstance(URI.create(path));
        } catch (IllegalArgumentException ex) {
            problem.setInstance(URI.create(UriUtils.encodePath(path, StandardCharsets.UTF_8)));
        }
        problem.setProperty("correlationId", RequestCorrelation.getOrCreateId(request));
        return problem;
    }

    static HttpHeaders headers(HttpHeaders original, HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(original);
        headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        headers.set(RequestCorrelation.HEADER, RequestCorrelation.getOrCreateId(request));
        return headers;
    }
}
