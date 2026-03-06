package com.backandwhite.domain.repository;

import com.backandwhite.domain.model.GatewayRoute;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Contrato de persistencia reactiva para las rutas del gateway.
 * Las implementaciones utilizan R2DBC sobre PostgreSQL.
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
}
