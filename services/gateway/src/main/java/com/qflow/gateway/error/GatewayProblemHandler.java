package com.qflow.gateway.error;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

import com.qflow.gateway.observability.RequestCorrelation;
import io.netty.handler.codec.DecoderException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponse;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Order(-2)
public class GatewayProblemHandler implements ErrorWebExceptionHandler, ServerResponse.Context {
    private static final Logger log = LoggerFactory.getLogger(GatewayProblemHandler.class);

    private final ServerCodecConfigurer codecs;

    public GatewayProblemHandler(ServerCodecConfigurer codecs) {
        this.codecs = codecs;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable failure) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(failure);
        }
        HttpStatusCode status = status(exchange, failure);
        String id = RequestCorrelation.getOrCreateId(exchange);
        if (status.is5xxServerError()) {
            var event =
                    log.atWarn()
                            .addKeyValue("correlationId", id)
                            .addKeyValue("status", status.value());
            if (!(failure instanceof ErrorResponse) || failure.getCause() != null) {
                event.setCause(failure);
            }
            event.log("Gateway request failed");
        }
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
                    case 404 ->
                            exchange.getAttribute(GATEWAY_ROUTE_ATTR) == null
                                    ? "No route matches the requested resource."
                                    : "The requested resource was not found.";
                    case 502 -> "The upstream service did not return a usable response.";
                    case 503 -> "The upstream service is temporarily unavailable.";
                    case 504 -> "The upstream service did not respond in time.";
                    default ->
                            status.is5xxServerError()
                                    ? "The server could not complete the request."
                                    : "The request could not be completed.";
                });
        String path = exchange.getRequest().getURI().getRawPath();
        problem.setInstance(URI.create(path.startsWith("//") ? "/%2F" + path.substring(2) : path));
        problem.setProperty("correlationId", id);

        var response = ServerResponse.status(status);
        if (failure instanceof ErrorResponse error) {
            response.headers(headers -> headers.addAll(error.getHeaders()));
        }
        exchange.getResponse().getHeaders().remove(HttpHeaders.CONTENT_LENGTH);
        return response.header(RequestCorrelation.HEADER, id)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyValue(problem)
                .flatMap(result -> result.writeTo(exchange, this));
    }

    private HttpStatusCode status(ServerWebExchange exchange, Throwable failure) {
        if (failure instanceof ErrorResponse error) {
            return error.getStatusCode();
        }
        if (exchange.getAttribute(GATEWAY_ROUTE_ATTR) != null) {
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                if (cause instanceof ConnectException
                        || cause instanceof UnknownHostException
                        || cause instanceof NoRouteToHostException) {
                    return HttpStatus.SERVICE_UNAVAILABLE;
                }
                if (cause instanceof IOException || cause instanceof DecoderException) {
                    status = HttpStatus.BAD_GATEWAY;
                }
            }
            return status;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    @Override
    public List<HttpMessageWriter<?>> messageWriters() {
        return codecs.getWriters();
    }

    @Override
    public List<ViewResolver> viewResolvers() {
        return List.of();
    }
}
