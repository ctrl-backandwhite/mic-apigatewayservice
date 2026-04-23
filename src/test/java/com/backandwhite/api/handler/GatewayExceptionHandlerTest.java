package com.backandwhite.api.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.backandwhite.common.exception.EntityNotFoundException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import reactor.test.StepVerifier;

class GatewayExceptionHandlerTest {

    private GatewayExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GatewayExceptionHandler();
    }

    @Test
    void handleEntityNotFound_shouldReturn404Dto() {
        EntityNotFoundException ex = new EntityNotFoundException("RT001", "Route not found");

        StepVerifier.create(handler.handleEntityNotFound(ex)).assertNext(dto -> {
            assertThat(dto.getCode()).isEqualTo("RT001");
            assertThat(dto.getMessage()).isEqualTo("Route not found");
            assertThat(dto.getDetails()).isEmpty();
            assertThat(dto.getDateTime()).isNotNull();
        }).verifyComplete();
    }

    @Test
    void handleResponseStatus_withReason_shouldUseReasonAsMessage() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service down");

        StepVerifier.create(handler.handleResponseStatus(ex)).assertNext(dto -> {
            assertThat(dto.getCode()).isEqualTo("GW503");
            assertThat(dto.getMessage()).isEqualTo("Service down");
        }).verifyComplete();
    }

    @Test
    void handleResponseStatus_withoutReason_shouldUseDefaultMessage() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND);

        StepVerifier.create(handler.handleResponseStatus(ex)).assertNext(dto -> {
            assertThat(dto.getCode()).isEqualTo("GW404");
            assertThat(dto.getMessage()).isEqualTo("Requested resource not found.");
        }).verifyComplete();
    }

    @Test
    void handleGeneral_shouldReturn500Dto() {
        Exception ex = new RuntimeException("Unexpected failure");

        StepVerifier.create(handler.handleGeneral(ex)).assertNext(dto -> {
            assertThat(dto.getCode()).isEqualTo("IS001");
            assertThat(dto.getMessage()).isEqualTo("An unexpected error occurred.");
            assertThat(dto.getDetails()).isEmpty();
        }).verifyComplete();
    }

    @Test
    void handleValidation_shouldReturn400WithFieldErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getAllErrors()).thenReturn(List.of(new FieldError("dto", "id", "Route id is required"),
                new FieldError("dto", "uri", "Route URI is required")));

        WebExchangeBindException ex = mock(WebExchangeBindException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(ex.getMessage()).thenReturn("Validation failed");

        StepVerifier.create(handler.handleValidation(ex)).assertNext(dto -> {
            assertThat(dto.getCode()).isEqualTo("VE001");
            assertThat(dto.getMessage()).isEqualTo("Validation error.");
            assertThat(dto.getDetails()).containsExactly("Route id is required", "Route URI is required");
        }).verifyComplete();
    }

    @Test
    void handleResponseStatus_withUnauthorized_shouldUseDefaultMessage() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        StepVerifier.create(handler.handleResponseStatus(ex)).assertNext(dto -> {
            assertThat(dto.getCode()).isEqualTo("GW401");
            assertThat(dto.getMessage()).isEqualTo("Unauthorized. Please log in.");
        }).verifyComplete();
    }

    @Test
    void handleResponseStatus_withForbidden_shouldUseDefaultMessage() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.FORBIDDEN);
        StepVerifier.create(handler.handleResponseStatus(ex)).assertNext(dto -> {
            assertThat(dto.getCode()).isEqualTo("GW403");
            assertThat(dto.getMessage()).isEqualTo("Access denied. You do not have sufficient permissions.");
        }).verifyComplete();
    }

    @Test
    void handleResponseStatus_withBadRequest_shouldUseDefaultMessage() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.BAD_REQUEST);
        StepVerifier.create(handler.handleResponseStatus(ex)).assertNext(dto -> {
            assertThat(dto.getCode()).isEqualTo("GW400");
            assertThat(dto.getMessage()).isEqualTo("Invalid request.");
        }).verifyComplete();
    }

    @Test
    void handleResponseStatus_withMethodNotAllowed_shouldUseDefaultMessage() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED);
        StepVerifier.create(handler.handleResponseStatus(ex)).assertNext(dto -> {
            assertThat(dto.getCode()).isEqualTo("GW405");
            assertThat(dto.getMessage()).isEqualTo("HTTP method not allowed.");
        }).verifyComplete();
    }

    @Test
    void handleResponseStatus_withTooManyRequests_shouldUseDefaultMessage() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
        StepVerifier.create(handler.handleResponseStatus(ex)).assertNext(dto -> {
            assertThat(dto.getCode()).isEqualTo("GW429");
            assertThat(dto.getMessage()).isEqualTo("Too many requests. Please wait a moment.");
        }).verifyComplete();
    }

    @Test
    void handleResponseStatus_withServiceUnavailable_shouldUseDefaultMessage() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        StepVerifier.create(handler.handleResponseStatus(ex)).assertNext(dto -> {
            assertThat(dto.getCode()).isEqualTo("GW503");
            assertThat(dto.getMessage()).isNotBlank();
        }).verifyComplete();
    }

    @Test
    void handleResponseStatus_withBlankReason_shouldUseDefaultMessage() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "  ");
        StepVerifier.create(handler.handleResponseStatus(ex)).assertNext(dto -> {
            assertThat(dto.getCode()).isEqualTo("GW404");
            assertThat(dto.getMessage()).isEqualTo("Requested resource not found.");
        }).verifyComplete();
    }

    @Test
    void handleResponseStatus_withDefaultStatus_shouldUseGenericMessage() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.CONFLICT);
        StepVerifier.create(handler.handleResponseStatus(ex)).assertNext(dto -> {
            assertThat(dto.getCode()).isEqualTo("GW409");
            assertThat(dto.getMessage()).isEqualTo("An unexpected error occurred.");
        }).verifyComplete();
    }

    @Test
    void handleResponseStatus_withUnknownStatusCode_shouldFallbackToInternalServerError() {
        ResponseStatusException ex = new ResponseStatusException(org.springframework.http.HttpStatusCode.valueOf(799),
                null);

        StepVerifier.create(handler.handleResponseStatus(ex)).assertNext(dto -> {
            assertThat(dto.getCode()).isEqualTo("GW500");
            assertThat(dto.getMessage()).isEqualTo("An unexpected error occurred.");
        }).verifyComplete();
    }
}
