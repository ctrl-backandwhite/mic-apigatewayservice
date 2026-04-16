package com.backandwhite.api.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import reactor.test.StepVerifier;

class GatewayWebExceptionHandlerTest {

    private GatewayWebExceptionHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        handler = new GatewayWebExceptionHandler(objectMapper);
    }

    @Test
    void handle_withJsonAccept_shouldReturnJson() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/unknown")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE).build());

        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "Route not found");

        StepVerifier.create(handler.handle(exchange, ex)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    }

    @Test
    void handle_withHtmlAccept_shouldReturnHtml() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/unknown-page")
                .header(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml").build());

        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND);

        StepVerifier.create(handler.handle(exchange, ex)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        MediaType contentType = exchange.getResponse().getHeaders().getContentType();
        assertThat(contentType).isNotNull();
        assertThat(contentType.getType()).isEqualTo("text");
        assertThat(contentType.getSubtype()).isEqualTo("html");
    }

    @Test
    void handle_withNoAcceptHeader_shouldReturnHtml() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/unknown").build());

        ResponseStatusException ex = new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);

        StepVerifier.create(handler.handle(exchange, ex)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void handle_withAllowedOrigin_shouldAddCorsHeaders() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/unknown")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.ORIGIN, "http://localhost:5174").build());

        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND);

        StepVerifier.create(handler.handle(exchange, ex)).verifyComplete();

        assertThat(exchange.getResponse().getHeaders().getFirst("Access-Control-Allow-Origin"))
                .isEqualTo("http://localhost:5174");
    }

    @Test
    void handle_withDisallowedOrigin_shouldNotAddCorsHeaders() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/unknown")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.ORIGIN, "http://evil.com").build());

        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND);

        StepVerifier.create(handler.handle(exchange, ex)).verifyComplete();

        assertThat(exchange.getResponse().getHeaders().getFirst("Access-Control-Allow-Origin")).isNull();
    }

    @Test
    void handle_withGenericException_shouldReturn500() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/test")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE).build());

        RuntimeException ex = new RuntimeException("Unexpected");

        StepVerifier.create(handler.handle(exchange, ex)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void handle_withUnauthorized_shouldReturnDefaultMessage() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/admin")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE).build());

        ResponseStatusException ex = new ResponseStatusException(HttpStatus.UNAUTHORIZED);

        StepVerifier.create(handler.handle(exchange, ex)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void handle_withForbidden_shouldReturnDefaultMessage() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/admin")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE).build());

        ResponseStatusException ex = new ResponseStatusException(HttpStatus.FORBIDDEN);

        StepVerifier.create(handler.handle(exchange, ex)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void handle_withBadRequest_shouldReturnDefaultMessage() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/test")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE).build());

        ResponseStatusException ex = new ResponseStatusException(HttpStatus.BAD_REQUEST);

        StepVerifier.create(handler.handle(exchange, ex)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handle_withMethodNotAllowed_shouldReturnDefaultMessage() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/test")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE).build());

        ResponseStatusException ex = new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED);

        StepVerifier.create(handler.handle(exchange, ex)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    void handle_withTooManyRequests_shouldReturnDefaultMessage() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/test")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE).build());

        ResponseStatusException ex = new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);

        StepVerifier.create(handler.handle(exchange, ex)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void handle_withServiceUnavailable_shouldReturnDefaultMessage() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/test")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE).build());

        ResponseStatusException ex = new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);

        StepVerifier.create(handler.handle(exchange, ex)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void handle_withExplicitReason_shouldUseReasonInsteadOfDefault() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/test")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE).build());

        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "Custom reason");

        StepVerifier.create(handler.handle(exchange, ex)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void handle_withAcceptBothHtmlAndJson_shouldReturnHtml() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/test")
                .header(HttpHeaders.ACCEPT, "text/html, application/json").build());

        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND);

        StepVerifier.create(handler.handle(exchange, ex)).verifyComplete();

        MediaType contentType = exchange.getResponse().getHeaders().getContentType();
        assertThat(contentType).isNotNull();
        assertThat(contentType.getType()).isEqualTo("text");
        assertThat(contentType.getSubtype()).isEqualTo("html");
    }

    @Test
    void handle_withExistingCorsHeader_shouldNotDuplicate() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/test").header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200").build());

        // Pre-set CORS header to simulate CorsWebFilter already ran
        exchange.getResponse().getHeaders().set("Access-Control-Allow-Origin", "http://localhost:4200");

        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND);

        StepVerifier.create(handler.handle(exchange, ex)).verifyComplete();

        assertThat(exchange.getResponse().getHeaders().getFirst("Access-Control-Allow-Origin"))
                .isEqualTo("http://localhost:4200");
    }

    @Test
    void handle_withWildcardAccept_shouldReturnHtml() {
        MockServerWebExchange exchange = MockServerWebExchange
                .from(MockServerHttpRequest.get("/api/v1/test").header(HttpHeaders.ACCEPT, "*/*").build());

        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND);

        StepVerifier.create(handler.handle(exchange, ex)).verifyComplete();

        MediaType contentType = exchange.getResponse().getHeaders().getContentType();
        assertThat(contentType).isNotNull();
        assertThat(contentType.getType()).isEqualTo("text");
    }
}
