package com.qflow.hello.api.error;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(HelloProblemDetailsTests.FailingController.class)
class HelloProblemDetailsTests {
    private static final String HEADER = "X-Correlation-Id";
    private static final String ID = "5b21cb92-349e-4db8-8db1-7f6dbbe389ae";

    @LocalServerPort private int port;
    private RestTestClient client;

    @BeforeEach
    void connect() {
        client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void unknownResourceUsesProblemDetailsWithGeneratedCorrelationId() {
        var response =
                client.get()
                        .uri("/missing?private=query")
                        .header(HEADER, "invalid")
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
                .isEqualTo("/missing")
                .jsonPath("$.correlationId")
                .isEqualTo(id);
    }

    @Test
    void preservesAllowHeaderForUnsupportedMethods() {
        client.post()
                .uri("/hello")
                .header(HEADER, ID)
                .exchange()
                .expectStatus()
                .isEqualTo(405)
                .expectHeader()
                .valueEquals(HttpHeaders.ALLOW, "GET")
                .expectHeader()
                .valueEquals(HEADER, ID)
                .expectBody()
                .jsonPath("$.correlationId")
                .isEqualTo(ID);
    }

    @Test
    void unexpectedAndServletErrorsUseSafeDetails() {
        for (String path : new String[] {"/test/unexpected", "/test/servlet-error"}) {
            var response =
                    client.get()
                            .uri(path)
                            .header(HEADER, ID)
                            .exchange()
                            .expectStatus()
                            .isEqualTo(500)
                            .expectHeader()
                            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                            .expectHeader()
                            .valueEquals(HEADER, ID)
                            .expectBody();
            response.jsonPath("$.detail")
                    .isEqualTo("The server could not complete the request.")
                    .jsonPath("$.instance")
                    .isEqualTo(path)
                    .jsonPath("$.correlationId")
                    .isEqualTo(ID);
            assertThat(response.returnResult().getResponseBody())
                    .asString()
                    .doesNotContain("SELECT", "private_table", "Exception", "trace");
        }
    }

    @Test
    void fieldErrorsOmitRejectedValuesAndValidationMessages() {
        var response =
                client.post()
                        .uri("/test/validation")
                        .header(HEADER, ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"name\":\"private-value\"}")
                        .exchange()
                        .expectStatus()
                        .isBadRequest()
                        .expectBody();
        response.jsonPath("$.type")
                .isEqualTo("https://qflow.local/problems/validation-error")
                .jsonPath("$.errors[0].field")
                .isEqualTo("name")
                .jsonPath("$.errors[0].code")
                .isEqualTo("InvalidName")
                .jsonPath("$.errors[0].message")
                .isEqualTo("Invalid value.");
        assertThat(response.returnResult().getResponseBody())
                .asString()
                .doesNotContain("private-value");
    }

    @RestController
    static class FailingController {
        @GetMapping("/test/unexpected")
        void unexpected() {
            throw new IllegalStateException("SELECT * FROM private_table");
        }

        @GetMapping("/test/servlet-error")
        void servletError(HttpServletResponse response) throws IOException {
            response.sendError(500, "SELECT * FROM private_table");
        }

        @InitBinder
        void initBinder(WebDataBinder binder) {
            binder.addValidators(
                    Validator.forInstanceOf(
                            Input.class,
                            (input, errors) ->
                                    errors.rejectValue("name", "InvalidName", input.name())));
        }

        @PostMapping("/test/validation")
        void validation(@Validated @RequestBody Input input) {}
    }

    record Input(String name) {}
}
