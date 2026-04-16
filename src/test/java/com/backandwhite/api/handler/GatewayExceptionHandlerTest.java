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
            assertThat(dto.getMessage()).isEqualTo("No se encontró el recurso solicitado.");
        }).verifyComplete();
    }

    @Test
    void handleGeneral_shouldReturn500Dto() {
        Exception ex = new RuntimeException("Unexpected failure");

        StepVerifier.create(handler.handleGeneral(ex)).assertNext(dto -> {
            assertThat(dto.getCode()).isEqualTo("IS001");
            assertThat(dto.getMessage()).isEqualTo("Ha ocurrido un error inesperado.");
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
            assertThat(dto.getMessage()).isEqualTo("Error de validación.");
            assertThat(dto.getDetails()).containsExactly("Route id is required", "Route URI is required");
        }).verifyComplete();
    }
}
