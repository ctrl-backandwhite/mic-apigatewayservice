package com.backandwhite.api.dto.out;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

/**
 * DTO de salida que representa la definición de una ruta tal como se expone a
 * través de la API del gateway.
 */
@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteDefinitionDtoOut {

    private String id;
    private String uri;
    private List<String> predicates;
    private List<String> filters;
    private int order;
    private boolean enabled;
    private Integer rateLimitReplenishRate;
    private Integer rateLimitBurstCapacity;
    private Integer rateLimitRequestedTokens;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
