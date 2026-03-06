package com.backandwhite.infrastructure.repository;

import com.backandwhite.infrastructure.entity.GatewayRouteEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

/**
 * Repositorio R2DBC reactivo para operaciones CRUD sobre {@link GatewayRouteEntity}.
 */
public interface GatewayRouteR2dbcRepository extends ReactiveCrudRepository<GatewayRouteEntity, String> {

    Flux<GatewayRouteEntity> findByEnabledTrue();
}
