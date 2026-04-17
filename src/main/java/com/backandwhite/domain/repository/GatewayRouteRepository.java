package com.backandwhite.domain.repository;

import com.backandwhite.domain.model.GatewayRoute;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive persistence contract for gateway routes. Implementations use R2DBC
 * over PostgreSQL.
 */
public interface GatewayRouteRepository {

    Flux<GatewayRoute> findAll();

    Flux<GatewayRoute> findAllEnabled();

    Mono<GatewayRoute> findById(String id);

    Mono<GatewayRoute> save(GatewayRoute route);

    Mono<GatewayRoute> update(GatewayRoute route, String id);

    Mono<Void> delete(String id);

    Mono<GatewayRoute> toggleEnabled(String id);

    Mono<Long> count();

    /**
     * Inserts the route if it does not exist or updates uri, predicates, filters,
     * order and rate-limit if it already exists. Preserves {@code enabled} and
     * {@code createdAt} so as not to overwrite manual operator changes.
     */
    Mono<Void> upsert(GatewayRoute route);
}
