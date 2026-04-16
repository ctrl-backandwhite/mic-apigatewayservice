package com.backandwhite.api.handler;

import com.backandwhite.api.dto.GatewayErrorResponseDto;
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

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

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
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/unknown")
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .build());

        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "Route not found");

        StepVerifier.create(handler.handle(exchange, ex))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    }

    @Test
    void handle_withHtmlAccept_shouldReturnHtml() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/unknown-page")
                        .header(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml")
                        .build());

        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND);

        StepVerifier.create(handler.handle(exchange, ex))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        MediaType contentType = exchange.getResponse().getHeaders().getContentType();
        assertThat(contentType).isNotNull();
        assertThat(contentType.getType()).isEqualTo("text");
        assertThat(contentType.getSubtype()).isEqualTo("html");
    }

    @Test
    void handle_withNoAcceptHeader_shouldReturnHtml() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/unknown").build());

        ResponseStatusException ex = new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);

        StepVerifier.create(handler.handle(exchange, ex))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void handle_withAllowedOrigin_shouldAddCorsHeaders() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/unknown")
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .header(HttpHeaders.ORIGIN, "http://localhost:5174")
                        .build());

        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND);

        StepVerifier.create(handler.handle(exchange, ex))
                .verifyComplete();

        assertThat(exchange.getResponse().getHeaders().getFirst("Access-Control-Allow-Origin"))
                .isEqualTo("http://localhost:5174");
    }

    @Test
    void handle_withDisallowedOrigin_shouldNotAddCorsHeaders() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/unknown")
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .header(HttpHeaders.ORIGIN, "http://evil.com")
                        .build());

        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND);

        StepVerifier.create(handler.handle(exchange, ex))
                .verifyComplete();

        assertThat(exchange.getResponse().getHeaders().getFirst("Access-Control-Allow-Origin"))
                .isNull();
    }

    @Test
    void handle_withGenericException_shouldReturn500() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/test")
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .build());

        RuntimeException ex = new RuntimeException("Unexpected");

        StepVerifier.create(handler.handle(exchange, ex))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
