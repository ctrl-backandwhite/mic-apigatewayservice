package com.backandwhite.infrastructure.repository;

import com.backandwhite.domain.model.GatewayRoute;
import com.backandwhite.domain.repository.GatewayRouteRepository;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Implementation of {@link RouteDefinitionRepository} that loads and persists
 * route definitions from PostgreSQL via R2DBC.
 *
 * <p>
 * By implementing this interface, Spring Cloud Gateway uses PostgreSQL as the
 * source of truth instead of YAML configuration files. Any change in the
 * database + publication of {@code RefreshRoutesEvent} reloads routes
 * immediately without restarting the service.
 *
 * <p>
 * If a route has rate-limit configuration ({@code rateLimitReplenishRate} and
 * {@code rateLimitBurstCapacity} not null), a {@code RequestRateLimiter} filter
 * is automatically injected with those values. This allows configuring
 * different rate limits per endpoint dynamically from the route management API.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class PostgresRouteDefinitionRepository implements RouteDefinitionRepository {

    private static final String RATE_LIMITER_FILTER = "RequestRateLimiter";

    private final GatewayRouteRepository routeRepository;

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        return routeRepository.findAllEnabled().map(this::toRouteDefinition).onErrorResume(e -> {
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
     * Builds the filters list for a route. Any explicit filters stored in the DB
     * are included first. If the route has rate-limit columns configured, a
     * {@code RequestRateLimiter} filter is appended with those values.
     */
    private List<FilterDefinition> toFilterDefinitions(GatewayRoute route) {
        List<FilterDefinition> filters = new ArrayList<>();

        // 1. Explicit filters from DB (e.g. StripPrefix, AddRequestHeader…)
        if (route.getFilters() != null) {
            route.getFilters().stream().map(FilterDefinition::new).forEach(filters::add);
        }

        // 2. Dynamic rate limiter (only when configured on the route)
        if (route.getRateLimitReplenishRate() != null && route.getRateLimitBurstCapacity() != null) {

            FilterDefinition rl = new FilterDefinition();
            rl.setName(RATE_LIMITER_FILTER);
            rl.addArg("redis-rate-limiter.replenishRate", String.valueOf(route.getRateLimitReplenishRate()));
            rl.addArg("redis-rate-limiter.burstCapacity", String.valueOf(route.getRateLimitBurstCapacity()));
            rl.addArg("redis-rate-limiter.requestedTokens", String
                    .valueOf(route.getRateLimitRequestedTokens() != null ? route.getRateLimitRequestedTokens() : 1));
            rl.addArg("key-resolver", "#{@ipKeyResolver}");
            rl.addArg("deny-empty-key", "true");
            filters.add(rl);
        }

        return filters;
    }

    private List<PredicateDefinition> toPredicateDefinitions(List<String> predicates) {
        if (predicates == null)
            return List.of();
        return predicates.stream().map(PredicateDefinition::new).toList();
    }
}
