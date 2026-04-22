package com.backandwhite.api.handler;

import com.backandwhite.api.dto.GatewayErrorResponseDto;
import com.backandwhite.common.exception.EntityNotFoundException;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * Global exception handler for the API Gateway (WebFlux).
 *
 * <p>
 * Converts domain exceptions to standard HTTP responses compatible with the
 * Spring WebFlux reactive model. Routing errors (e.g. 404 with no matching
 * route) are intercepted by {@link GatewayWebExceptionHandler} before reaching
 * this handler.
 *
 * @author NX036-ALFA
 */
@Log4j2
@RestControllerAdvice
public class GatewayExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<GatewayErrorResponseDto> handleEntityNotFound(EntityNotFoundException ex) {
        log.warn("Entity not found: {}", ex.getMessage());
        return Mono.just(errorBody(HttpStatus.NOT_FOUND, ex.getCode(), ex.getMessage(), List.of()));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<GatewayErrorResponseDto> handleValidation(WebExchangeBindException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        List<String> details = ex.getBindingResult().getAllErrors().stream().map(e -> e.getDefaultMessage()).toList();
        return Mono.just(errorBody(HttpStatus.BAD_REQUEST, "VE001", "Validation error.", details));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Mono<GatewayErrorResponseDto> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String message = (ex.getReason() != null && !ex.getReason().isBlank())
                ? ex.getReason()
                : resolveDefaultMessage(status);
        log.debug("Response status exception: {} — {}", status.value(), message);
        return Mono.just(errorBody(status, "GW" + status.value(), message, List.of()));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<GatewayErrorResponseDto> handleGeneral(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return Mono
                .just(errorBody(HttpStatus.INTERNAL_SERVER_ERROR, "IS001", "An unexpected error occurred.", List.of()));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private GatewayErrorResponseDto errorBody(HttpStatus status, String code, String message, List<String> details) {
        return GatewayErrorResponseDto.builder().code(code).message(message).details(details)
                .dateTime(ZonedDateTime.now()).build();
    }

    private String resolveDefaultMessage(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "Requested resource not found.";
            case UNAUTHORIZED -> "Unauthorized. Please log in.";
            case FORBIDDEN -> "Access denied. You do not have sufficient permissions.";
            case BAD_REQUEST -> "Invalid request.";
            case METHOD_NOT_ALLOWED -> "HTTP method not allowed.";
            case TOO_MANY_REQUESTS -> "Too many requests. Please wait a moment.";
            case SERVICE_UNAVAILABLE -> "Service temporarily unavailable.";
            default -> "An unexpected error occurred.";
        };
    }
}
