package com.backandwhite.api.dto.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

/**
 * Input DTO for registering or updating a dynamic route in the gateway.
 * <p>
 * Predicates and filters format: Spring Cloud Gateway shortcut.
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
