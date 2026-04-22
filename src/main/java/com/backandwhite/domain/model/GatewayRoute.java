package com.backandwhite.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

/**
 * Domain model representing an API Gateway route.
 *
 * <p>
 * Predicates and filters are stored in Spring Cloud Gateway shortcut format:
 * <ul>
 * <li>Predicate: {@code "Path=/api/v1/catalog/**"}</li>
 * <li>Filter: {@code "RequestRateLimiter=10,20"}</li>
 * </ul>
 */
@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayRoute {

    private String id;
    private String uri;
    private List<String> predicates;
    private List<String> filters;
    private int order;
    private boolean enabled;

    /** Tokens added per second (sustained rate). null = no rate limit. */
    private Integer rateLimitReplenishRate;

    /** Maximum bucket capacity (peak burst). null = no rate limit. */
    private Integer rateLimitBurstCapacity;

    /** Tokens consumed per request. Defaults to 1 when rate limit is configured. */
    private Integer rateLimitRequestedTokens;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
