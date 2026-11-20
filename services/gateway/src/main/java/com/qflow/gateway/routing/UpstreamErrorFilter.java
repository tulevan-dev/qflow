package com.qflow.gateway.routing;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CLIENT_RESPONSE_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CLIENT_RESPONSE_CONN_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CLIENT_RESPONSE_HEADER_NAMES;

import com.qflow.gateway.observability.RequestCorrelation;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.zip.GZIPInputStream;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.NettyWriteResponseFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.node.ObjectNode;

@Component
public class UpstreamErrorFilter implements GlobalFilter, Ordered {
    private static final List<String> ERROR_HEADERS =
            List.of(HttpHeaders.ALLOW, HttpHeaders.RETRY_AFTER, HttpHeaders.WWW_AUTHENTICATE);
    private static final int MAX_PROBLEM_BODY_BYTES = 64 * 1024;
    private static final Duration PROBLEM_BODY_TIMEOUT = Duration.ofSeconds(2);
    private static final byte[] EMPTY_BODY = new byte[0];
    private final ObjectMapper mapper;
    private final ObjectReader problemReader;

    public UpstreamErrorFilter(ObjectMapper mapper) {
        this.mapper = mapper;
        this.problemReader = mapper.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    }

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
                            if (response.isCommitted()
                                    || status == null
                                    || !status.isError()
                                    || exchange.getAttribute(CLIENT_RESPONSE_ATTR) == null) {
                                return Mono.empty();
                            }
                            if (status.is4xxClientError()
                                    && hasProblemBody(response.getHeaders())) {
                                if (HttpMethod.HEAD.equals(exchange.getRequest().getMethod())) {
                                    return Mono.empty();
                                }
                                return normalizeProblemBody(exchange, status);
                            }
                            return replaceError(exchange, status);
                        }));
    }

    private Mono<Void> normalizeProblemBody(ServerWebExchange exchange, HttpStatusCode status) {
        Connection connection = exchange.getAttribute(CLIENT_RESPONSE_CONN_ATTR);
        if (connection == null) {
            return replaceError(exchange, status);
        }
        var response = exchange.getResponse();
        var factory = response.bufferFactory();
        var encodings = response.getHeaders().getOrEmpty(HttpHeaders.CONTENT_ENCODING);
        return DataBufferUtils.join(
                        connection.inbound().receive().asByteArray().map(factory::wrap),
                        MAX_PROBLEM_BODY_BYTES)
                .map(buffer -> {
                    try {
                        byte[] bytes = new byte[buffer.readableByteCount()];
                        buffer.read(bytes);
                        return bytes;
                    } finally {
                        DataBufferUtils.release(buffer);
                    }
                })
                .defaultIfEmpty(EMPTY_BODY)
                .timeout(PROBLEM_BODY_TIMEOUT)
                .flatMap(bytes -> {
                    // The upstream body was fully consumed. Skip the Netty writer's second read.
                    exchange.getAttributes().remove(CLIENT_RESPONSE_CONN_ATTR);
                    byte[] normalized =
                            normalize(
                                    bytes,
                                    encodings,
                                    status,
                                    RequestCorrelation.getOrCreateId(exchange));
                    if (normalized == null) {
                        return replaceError(exchange, status);
                    }
                    var headers = response.getHeaders();
                    headers.remove(HttpHeaders.CONTENT_ENCODING);
                    headers.remove(HttpHeaders.TRANSFER_ENCODING);
                    headers.remove(HttpHeaders.ETAG);
                    headers.remove("Content-MD5");
                    headers.remove("Digest");
                    headers.remove("Content-Digest");
                    headers.remove("Repr-Digest");
                    headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
                    headers.setContentLength(normalized.length);
                    return response.writeWith(Mono.fromSupplier(() -> factory.wrap(normalized)));
                })
                .onErrorResume(
                        ex ->
                                ex instanceof DataBufferLimitException
                                        || ex instanceof TimeoutException,
                        ex -> replaceError(exchange, status));
    }

    private byte[] normalize(
            byte[] bytes, List<String> encodings, HttpStatusCode status, String correlationId) {
        try {
            byte[] json = decodeBody(bytes, encodings);
            if (json == null
                    || !(problemReader.readTree(json) instanceof ObjectNode problem)
                    || !problem.path("type").isString()
                    || !problem.path("title").isString()
                    || !problem.path("status").isIntegralNumber()
                    || !problem.path("status").canConvertToInt()
                    || problem.path("status").intValue() != status.value()
                    || !isOptionalText(problem.path("detail"))
                    || !isOptionalText(problem.path("instance"))
                    || !hasValidErrors(problem.path("errors"))) {
                return null;
            }
            problem.put("correlationId", correlationId);
            return mapper.writeValueAsBytes(problem);
        } catch (JacksonException | IOException ex) {
            return null;
        }
    }

    private byte[] decodeBody(byte[] bytes, List<String> encodings) throws IOException {
        if (encodings.isEmpty()) {
            return bytes;
        }
        if (encodings.size() != 1) {
            return null;
        }
        String encoding = encodings.getFirst().trim();
        if ("identity".equalsIgnoreCase(encoding)) {
            return bytes;
        }
        if (!"gzip".equalsIgnoreCase(encoding)) {
            return null;
        }
        try (var gzip = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            byte[] decoded = gzip.readNBytes(MAX_PROBLEM_BODY_BYTES + 1);
            return decoded.length <= MAX_PROBLEM_BODY_BYTES ? decoded : null;
        }
    }

    private boolean isOptionalText(JsonNode node) {
        return node.isMissingNode() || node.isString();
    }

    private boolean hasValidErrors(JsonNode errors) {
        if (errors.isMissingNode()) {
            return true;
        }
        if (!errors.isArray()) {
            return false;
        }
        for (JsonNode error : errors) {
            if (!error.path("field").isString()
                    || !error.path("code").isString()
                    || !error.path("message").isString()) {
                return false;
            }
        }
        return true;
    }

    private Mono<Void> replaceError(ServerWebExchange exchange, HttpStatusCode status) {
        var headers = exchange.getResponse().getHeaders();
        var error = new ErrorResponseException(status);
        for (String name : ERROR_HEADERS) {
            List<String> values = headers.get(name);
            if (values != null) {
                error.getHeaders().put(name, List.copyOf(values));
            }
        }
        var upstreamHeaders = exchange.<Iterable<String>>getAttribute(CLIENT_RESPONSE_HEADER_NAMES);
        if (upstreamHeaders != null) {
            upstreamHeaders.forEach(headers::remove);
        }
        return Mono.error(error);
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
