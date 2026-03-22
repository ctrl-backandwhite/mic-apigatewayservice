package com.backandwhite.api.dto.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import java.util.List;

/**
 * DTO de entrada para registrar o actualizar una ruta dinámica en el gateway.
 * <p>
 * Formato de predicates y filters: shortcut de Spring Cloud Gateway.
 * 
 * <pre>
 * {
 *   "id": "new-service",
 *   "uri": "http://localhost:9099",
 *   "predicates": ["Path=/api/v1/new/**"],
 *   "filters": [],
 *   "order": 0
 * }
 * </pre>
 */
@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteDefinitionDtoIn {

    @NotBlank(message = "Route id is required")
    private String id;

    @NotBlank(message = "Route URI is required")
    private String uri;

    @NotEmpty(message = "At least one predicate is required")
    private List<String> predicates;

    private List<String> filters;

    private int order;

    /** Tokens added per second. null = no rate limit on this route. */
    private Integer rateLimitReplenishRate;

    /** Maximum bucket capacity. null = no rate limit on this route. */
    private Integer rateLimitBurstCapacity;

    /** Tokens consumed per request. Defaults to 1. */
    private Integer rateLimitRequestedTokens;
}
