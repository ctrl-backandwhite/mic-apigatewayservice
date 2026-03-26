package com.backandwhite.api.handler;

import com.backandwhite.common.exception.EntityNotFoundException;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manejador global de excepciones para el API Gateway (WebFlux).
 *
 * <p>
 * Convierte las excepciones de dominio en respuestas HTTP estándar
 * de forma compatible con el modelo reactivo de Spring WebFlux.
 */
@Log4j2
@RestControllerAdvice
public class GatewayExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<Map<String, Object>> handleEntityNotFound(EntityNotFoundException ex) {
        log.warn("Entity not found: {}", ex.getMessage());
        return Mono.just(errorBody(HttpStatus.NOT_FOUND, ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Map<String, Object>> handleValidation(WebExchangeBindException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        List<String> details = ex.getBindingResult().getAllErrors()
                .stream()
                .map(e -> e.getDefaultMessage())
                .toList();
        Map<String, Object> body = errorBody(HttpStatus.BAD_REQUEST, "VE001", "Validation error");
        body.put("details", details);
        return Mono.just(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Mono<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        log.debug("Response status exception: {} {}", status.value(), ex.getReason());
        return Mono.just(errorBody(status, "GW" + status.value(), ex.getReason()));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<Map<String, Object>> handleGeneral(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return Mono.just(errorBody(HttpStatus.INTERNAL_SERVER_ERROR, "IS001", "Internal server error"));
    }

    private Map<String, Object> errorBody(HttpStatus status, String code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", ZonedDateTime.now().toString());
        body.put("status", status.value());
        body.put("code", code);
        body.put("message", message);
        return body;
    }
}
