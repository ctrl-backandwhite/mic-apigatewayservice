package com.backandwhite.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Modelo de dominio que representa una ruta del API Gateway.
 *
 * <p>Las predicates y filters se almacenan en formato shortcut de Spring Cloud Gateway:
 * <ul>
 *   <li>Predicate: {@code "Path=/api/v1/catalog/**"}</li>
 *   <li>Filter:    {@code "RequestRateLimiter=10,20"}</li>
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
