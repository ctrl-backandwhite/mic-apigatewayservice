package com.backandwhite.infrastructure.repository;

import com.backandwhite.domain.model.GatewayRoute;
import com.backandwhite.domain.repository.GatewayRouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de {@link RouteDefinitionRepository} que carga y persiste
 * las definiciones de rutas desde PostgreSQL mediante R2DBC.
 *
 * <p>
 * Al implementar esta interfaz, Spring Cloud Gateway usa PostgreSQL como
 * fuente de verdad en lugar de los ficheros de configuración YAML.
 * Cualquier cambio en la base de datos + publicación de
 * {@code RefreshRoutesEvent}
 * recarga las rutas de forma inmediata sin reiniciar el servicio.
 *
 * <p>
 * Si una ruta tiene configuración de rate limit ({@code rateLimitReplenishRate}
 * y {@code rateLimitBurstCapacity} no nulos), se inyecta automáticamente un
 * {@code RequestRateLimiter} filter con esos valores. Esto permite configurar
 * límites de tasa distintos por endpoint de forma dinámica desde la API de
 * gestión de rutas.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class PostgresRouteDefinitionRepository implements RouteDefinitionRepository {

    private static final String RATE_LIMITER_FILTER = "RequestRateLimiter";

    private final GatewayRouteRepository routeRepository;

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        return routeRepository.findAllEnabled()
                .map(this::toRouteDefinition)
                .onErrorResume(e -> {
                    log.error("Failed to load routes from PostgreSQL: {}", e.getMessage());
                    return Flux.empty();
                });
    }

    @Override
    public Mono<Void> save(Mono<RouteDefinition> route) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> delete(Mono<String> routeId) {
        return Mono.empty();
    }

    // ------------------------------------------------------------------
    // Conversion helpers
    // ------------------------------------------------------------------

    private RouteDefinition toRouteDefinition(GatewayRoute route) {
        RouteDefinition definition = new RouteDefinition();
        definition.setId(route.getId());
        definition.setUri(URI.create(route.getUri()));
        definition.setOrder(route.getOrder());
        definition.setPredicates(toPredicateDefinitions(route.getPredicates()));
        definition.setFilters(toFilterDefinitions(route));
        return definition;
    }

    /**
     * Builds the filters list for a route. Any explicit filters stored in the
     * DB are included first. If the route has rate-limit columns configured,
     * a {@code RequestRateLimiter} filter is appended with those values.
     */
    private List<FilterDefinition> toFilterDefinitions(GatewayRoute route) {
        List<FilterDefinition> filters = new ArrayList<>();

        // 1. Explicit filters from DB (e.g. StripPrefix, AddRequestHeader…)
        if (route.getFilters() != null) {
            route.getFilters().stream()
                    .map(FilterDefinition::new)
                    .forEach(filters::add);
        }

        // 2. Dynamic rate limiter (only when configured on the route)
        if (route.getRateLimitReplenishRate() != null
                && route.getRateLimitBurstCapacity() != null) {

            FilterDefinition rl = new FilterDefinition();
            rl.setName(RATE_LIMITER_FILTER);
            rl.addArg("redis-rate-limiter.replenishRate",
                    String.valueOf(route.getRateLimitReplenishRate()));
            rl.addArg("redis-rate-limiter.burstCapacity",
                    String.valueOf(route.getRateLimitBurstCapacity()));
            rl.addArg("redis-rate-limiter.requestedTokens",
                    String.valueOf(route.getRateLimitRequestedTokens() != null
                            ? route.getRateLimitRequestedTokens()
                            : 1));
            rl.addArg("key-resolver", "#{@ipKeyResolver}");
            rl.addArg("deny-empty-key", "true");
            filters.add(rl);
        }

        return filters;
    }

    /**
     * Converts stored predicate strings to {@link PredicateDefinition} objects.
     *
     * <p>
     * Multiple {@code Path=} predicates are merged into a single one with
     * comma-separated patterns so Spring Cloud Gateway applies <strong>OR</strong>
     * logic instead of <strong>AND</strong>. Other predicate types are kept
     * as-is.
     */
    private List<PredicateDefinition> toPredicateDefinitions(List<String> predicates) {
        if (predicates == null || predicates.isEmpty()) {
            return List.of();
        }

        List<String> pathPatterns = new ArrayList<>();
        List<PredicateDefinition> others = new ArrayList<>();

        for (String p : predicates) {
            if (p.startsWith("Path=")) {
                pathPatterns.add(p.substring("Path=".length()));
            } else {
                others.add(new PredicateDefinition(p));
            }
        }

        List<PredicateDefinition> result = new ArrayList<>(others);
        if (!pathPatterns.isEmpty()) {
            result.addFirst(new PredicateDefinition("Path=" + String.join(",", pathPatterns)));
        }
        return result;
    }
}
