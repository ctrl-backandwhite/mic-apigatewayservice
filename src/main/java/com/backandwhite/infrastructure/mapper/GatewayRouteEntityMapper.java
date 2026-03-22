package com.backandwhite.infrastructure.mapper;

import com.backandwhite.domain.model.GatewayRoute;
import com.backandwhite.infrastructure.entity.GatewayRouteEntity;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper manual entre {@link GatewayRouteEntity} y {@link GatewayRoute}.
 * Se implementa manualmente (no MapStruct) porque la conversión JSON ↔
 * List<String>
 * requiere el ObjectMapper de Jackson.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class GatewayRouteEntityMapper {

    private static final TypeReference<List<String>> LIST_STRING_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public GatewayRoute toDomain(GatewayRouteEntity entity) {
        return GatewayRoute.builder()
                .id(entity.getId())
                .uri(entity.getUri())
                .predicates(parseJson(entity.getPredicates()))
                .filters(parseJson(entity.getFilters()))
                .order(entity.getOrder())
                .enabled(entity.isEnabled())
                .rateLimitReplenishRate(entity.getRateLimitReplenishRate())
                .rateLimitBurstCapacity(entity.getRateLimitBurstCapacity())
                .rateLimitRequestedTokens(entity.getRateLimitRequestedTokens())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public GatewayRouteEntity toEntity(GatewayRoute route) {
        return GatewayRouteEntity.builder()
                .id(route.getId())
                .uri(route.getUri())
                .predicates(toJson(route.getPredicates()))
                .filters(toJson(route.getFilters() != null ? route.getFilters() : List.of()))
                .order(route.getOrder())
                .enabled(route.isEnabled())
                .rateLimitReplenishRate(route.getRateLimitReplenishRate())
                .rateLimitBurstCapacity(route.getRateLimitBurstCapacity())
                .rateLimitRequestedTokens(route.getRateLimitRequestedTokens())
                .createdAt(route.getCreatedAt())
                .updatedAt(route.getUpdatedAt())
                .build();
    }

    private List<String> parseJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, LIST_STRING_TYPE);
        } catch (JacksonException e) {
            log.warn("Failed to parse JSON '{}': {}", json, e.getMessage());
            return List.of();
        }
    }

    private String toJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JacksonException e) {
            log.error("Failed to serialize list to JSON: {}", e.getMessage());
            return "[]";
        }
    }
}
