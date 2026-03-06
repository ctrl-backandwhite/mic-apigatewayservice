package com.backandwhite.infrastructure.repository;

import com.backandwhite.domain.repository.GatewayRouteRepository;
import com.backandwhite.infrastructure.mapper.GatewayRouteEntityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

/**
 * Implementación de {@link RouteDefinitionRepository} que carga y persiste
 * las definiciones de rutas desde PostgreSQL mediante R2DBC.
 *
 * <p>Al implementar esta interfaz, Spring Cloud Gateway usa PostgreSQL como
 * fuente de verdad en lugar de los ficheros de configuración YAML.
 * Cualquier cambio en la base de datos + publicación de {@code RefreshRoutesEvent}
 * recarga las rutas de forma inmediata sin reiniciar el servicio.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class PostgresRouteDefinitionRepository implements RouteDefinitionRepository {

    private final GatewayRouteRepository routeRepository;

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        return routeRepository.findAllEnabled()
                .map(route -> {
                    RouteDefinition definition = new RouteDefinition();
                    definition.setId(route.getId());
                    definition.setUri(URI.create(route.getUri()));
                    definition.setOrder(route.getOrder());
                    definition.setPredicates(toPredicateDefinitions(route.getPredicates()));
                    definition.setFilters(toFilterDefinitions(route.getFilters()));
                    return definition;
                })
                .onErrorResume(e -> {
                    log.error("Failed to load routes from PostgreSQL: {}", e.getMessage());
                    return Flux.empty();
                });
    }

    @Override
    public Mono<Void> save(Mono<RouteDefinition> route) {
        // Delegado a GatewayRouteRepositoryImpl vía RouteManagementUseCase
        return Mono.empty();
    }

    @Override
    public Mono<Void> delete(Mono<String> routeId) {
        // Delegado a GatewayRouteRepositoryImpl vía RouteManagementUseCase
        return Mono.empty();
    }

    private List<PredicateDefinition> toPredicateDefinitions(List<String> predicates) {
        if (predicates == null) return List.of();
        return predicates.stream()
                .map(PredicateDefinition::new)
                .toList();
    }

    private List<FilterDefinition> toFilterDefinitions(List<String> filters) {
        if (filters == null) return List.of();
        return filters.stream()
                .map(FilterDefinition::new)
                .toList();
    }
}
