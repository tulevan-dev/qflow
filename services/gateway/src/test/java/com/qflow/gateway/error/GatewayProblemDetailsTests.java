package com.qflow.gateway.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.tcp.TcpServer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayProblemDetailsTests {
    private static final String HEADER = "X-Correlation-Id";
    private static final String ID = "5b21cb92-349e-4db8-8db1-7f6dbbe389ae";
    private static final DisposableServer INVALID_UPSTREAM =
            TcpServer.create()
                    .host("127.0.0.1")
                    .port(0)
                    .handle((request, response) ->
                            response.sendString(Mono.just("not an HTTP response\r\n\r\n")))
                    .bindNow();
    private static final DisposableServer UPSTREAM =
            HttpServer.create()
                    .host("127.0.0.1")
                    .port(0)
                    .handle((request, response) -> {
                        String status = request.requestHeaders().get("X-Test-Upstream-Status");
                        if (status == null) {
                            return response.sendString(
                                    Mono.just(request.requestHeaders().get(HEADER)));
                        }
                        if (status.equals("400")) {
                            return response.status(400)
                                    .header("Content-Type", "application/problem+json")
                                    .sendString(
                                            Mono.just(
                                                    """
                                                    {
                                                      "type": "https://qflow.local/problems/validation-error",
                                                      "title": "Validation failed",
                                                      "status": 400,
                                                      "detail": "Request contains invalid fields",
                                                      "instance": "/hello",
                                                      "correlationId": "%s",
                                                      "errors": [{"field": "name", "code": "NotBlank", "message": "Invalid value."}]
                                                    }
                                                    """
                                                            .formatted(
                                                                    request.requestHeaders()
                                                                            .get(HEADER))));
                        }
                        if (status.equals("503")) {
                            response.header("Retry-After", "5");
                        }
                        return response.status(Integer.parseInt(status))
                                .header("X-Upstream-Internal", "private-host")
                                .sendString(Mono.just("SELECT * FROM private_table"));
                    })
                    .bindNow();

    @LocalServerPort private int port;
    private WebTestClient client;

    @DynamicPropertySource
    static void configureRoutes(DynamicPropertyRegistry properties) {
        var offline = HttpServer.create().host("127.0.0.1").port(0).bindNow();
        int offlinePort = offline.port();
        offline.disposeNow();
        String routes = "spring.cloud.gateway.server.webflux.routes";
        properties.add(routes + "[0].id", () -> "hello-route");
        properties.add(routes + "[0].uri", () -> "http://127.0.0.1:" + UPSTREAM.port());
        properties.add(routes + "[0].predicates[0]", () -> "Path=/hello");
        properties.add(routes + "[1].id", () -> "offline-route");
        properties.add(routes + "[1].uri", () -> "http://127.0.0.1:" + offlinePort);
        properties.add(routes + "[1].predicates[0]", () -> "Path=/offline");
        properties.add(routes + "[2].id", () -> "invalid-upstream-route");
        properties.add(routes + "[2].uri", () -> "http://127.0.0.1:" + INVALID_UPSTREAM.port());
        properties.add(routes + "[2].predicates[0]", () -> "Path=/invalid-upstream");
    }

    @AfterAll
    static void stopUpstream() {
        UPSTREAM.disposeNow();
        INVALID_UPSTREAM.disposeNow();
    }

    @BeforeEach
    void connect() {
        client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void missingRouteUsesProblemDetailsWithGeneratedCorrelationId() {
        var response =
                client.get()
                        .uri("/unknown?private=query")
                        .accept(MediaType.TEXT_HTML)
                        .exchange()
                        .expectStatus()
                        .isNotFound()
                        .expectHeader()
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .expectBody();
        String id = response.returnResult().getResponseHeaders().getFirst(HEADER);
        assertThat(UUID.fromString(id).toString()).isEqualTo(id);
        response.jsonPath("$.type")
                .isEqualTo("https://qflow.local/problems/not-found")
                .jsonPath("$.title")
                .isEqualTo("Not Found")
                .jsonPath("$.status")
                .isEqualTo(404)
                .jsonPath("$.detail")
                .exists()
                .jsonPath("$.instance")
                .isEqualTo("/unknown")
                .jsonPath("$.correlationId")
                .isEqualTo(id);
    }

    @ParameterizedTest
    @CsvSource({"404,not-found", "502,bad-gateway", "503,service-unavailable"})
    void replacesUpstreamErrorBodyAndHeaders(int status, String type) {
        var response =
                client.get()
                        .uri("/hello")
                        .header(HEADER, ID)
                        .header("X-Test-Upstream-Status", Integer.toString(status))
                        .exchange()
                        .expectStatus()
                        .isEqualTo(status)
                        .expectHeader()
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .expectHeader()
                        .valueEquals(HEADER, ID)
                        .expectHeader()
                        .doesNotExist("X-Upstream-Internal")
                        .expectBody();
        if (status == 503) {
            assertThat(response.returnResult().getResponseHeaders().getFirst("Retry-After"))
                    .isEqualTo("5");
        }
        response.jsonPath("$.type")
                .isEqualTo("https://qflow.local/problems/" + type)
                .jsonPath("$.status")
                .isEqualTo(status)
                .jsonPath("$.instance")
                .isEqualTo("/hello")
                .jsonPath("$.correlationId")
                .isEqualTo(ID);
        assertThat(response.returnResult().getResponseBody())
                .asString()
                .doesNotContain("SELECT", "private_table", "Exception", "trace");
    }

    @Test
    void refusedUpstreamConnectionReturnsServiceUnavailable() {
        client.get()
                .uri("/offline")
                .header(HEADER, ID)
                .exchange()
                .expectStatus()
                .isEqualTo(503)
                .expectHeader()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectHeader()
                .valueEquals(HEADER, ID)
                .expectBody()
                .jsonPath("$.type")
                .isEqualTo("https://qflow.local/problems/service-unavailable")
                .jsonPath("$.correlationId")
                .isEqualTo(ID);
    }

    @Test
    void invalidUpstreamResponseReturnsBadGateway() {
        client.get()
                .uri("/invalid-upstream")
                .header(HEADER, ID)
                .exchange()
                .expectStatus()
                .isEqualTo(502)
                .expectHeader()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectHeader()
                .valueEquals(HEADER, ID)
                .expectBody()
                .jsonPath("$.type")
                .isEqualTo("https://qflow.local/problems/bad-gateway")
                .jsonPath("$.correlationId")
                .isEqualTo(ID);
    }

    @Test
    void forwardsCorrelationIdOnSuccessfulRequests() {
        client.get()
                .uri("/hello")
                .header(HEADER, ID)
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .valueEquals(HEADER, ID)
                .expectBody(String.class)
                .isEqualTo(ID);
    }

    @Test
    void preservesUpstreamValidationErrors() {
        client.get()
                .uri("/hello")
                .header(HEADER, ID)
                .header("X-Test-Upstream-Status", "400")
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectHeader()
                .valueEquals(HEADER, ID)
                .expectBody()
                .jsonPath("$.correlationId")
                .isEqualTo(ID)
                .jsonPath("$.errors[0].field")
                .isEqualTo("name");
    }
}
