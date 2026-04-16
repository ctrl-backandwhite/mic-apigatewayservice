package com.backandwhite.infrastructure.repository.impl;

import com.backandwhite.domain.model.GatewayRoute;
import com.backandwhite.domain.repository.GatewayRouteRepository;
import com.backandwhite.infrastructure.entity.GatewayRouteEntity;
import com.backandwhite.infrastructure.mapper.GatewayRouteEntityMapper;
import com.backandwhite.infrastructure.repository.GatewayRouteR2dbcRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class GatewayRouteRepositoryImpl implements GatewayRouteRepository {

    private final GatewayRouteR2dbcRepository r2dbcRepository;
    private final GatewayRouteEntityMapper entityMapper;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;
    private final DatabaseClient databaseClient;

    @Override
    public Flux<GatewayRoute> findAll() {
        return r2dbcRepository.findAll().map(entityMapper::toDomain);
    }

    @Override
    public Flux<GatewayRoute> findAllEnabled() {
        return r2dbcRepository.findByEnabledTrue().map(entityMapper::toDomain);
    }

    @Override
    public Mono<GatewayRoute> findById(String id) {
        return r2dbcRepository.findById(id).map(entityMapper::toDomain);
    }

    @Override
    public Mono<GatewayRoute> save(GatewayRoute route) {
        GatewayRouteEntity entity = entityMapper.toEntity(route);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return r2dbcEntityTemplate.insert(entity).map(entityMapper::toDomain);
    }

    @Override
    public Mono<GatewayRoute> update(GatewayRoute route, String id) {
        return r2dbcRepository.findById(id).flatMap(existing -> {
            GatewayRouteEntity updated = entityMapper.toEntity(route);
            updated.setId(id);
            updated.setCreatedAt(existing.getCreatedAt());
            updated.setUpdatedAt(LocalDateTime.now());
            return r2dbcRepository.save(updated);
        }).map(entityMapper::toDomain);
    }

    @Override
    public Mono<Void> delete(String id) {
        return r2dbcRepository.deleteById(id);
    }

    @Override
    public Mono<GatewayRoute> toggleEnabled(String id) {
        return r2dbcRepository.findById(id).flatMap(entity -> {
            entity.setEnabled(!entity.isEnabled());
            entity.setUpdatedAt(LocalDateTime.now());
            return r2dbcRepository.save(entity);
        }).map(entityMapper::toDomain);
    }

    @Override
    public Mono<Long> count() {
        return r2dbcRepository.count();
    }

    /**
     * INSERT ... ON CONFLICT (id) DO UPDATE: - Si la ruta no existe → la inserta
     * con enabled=true y createdAt=NOW(). - Si ya existe → actualiza uri,
     * predicates, filters, order y rate-limit. Preserva enabled y createdAt para no
     * pisar cambios manuales.
     */
    @Override
    public Mono<Void> upsert(GatewayRoute route) {
        GatewayRouteEntity entity = entityMapper.toEntity(route);
        return databaseClient.sql("""
                INSERT INTO gateway_route
                    (id, uri, predicates, filters, route_order, enabled,
                     rate_limit_replenish_rate, rate_limit_burst_capacity,
                     rate_limit_requested_tokens, created_at, updated_at)
                VALUES
                    (:id, :uri, :predicates, :filters, :order, true,
                     :replenishRate, :burstCapacity, :requestedTokens,
                     NOW(), NOW())
                ON CONFLICT (id) DO UPDATE SET
                    uri                        = EXCLUDED.uri,
                    predicates                 = EXCLUDED.predicates,
                    filters                    = EXCLUDED.filters,
                    route_order                = EXCLUDED.route_order,
                    rate_limit_replenish_rate  = EXCLUDED.rate_limit_replenish_rate,
                    rate_limit_burst_capacity  = EXCLUDED.rate_limit_burst_capacity,
                    rate_limit_requested_tokens = EXCLUDED.rate_limit_requested_tokens,
                    updated_at                 = NOW()
                """).bind("id", entity.getId()).bind("uri", entity.getUri()).bind("predicates", entity.getPredicates())
                .bind("filters", entity.getFilters()).bind("order", entity.getOrder())
                .bind("replenishRate",
                        entity.getRateLimitReplenishRate() != null
                                ? entity.getRateLimitReplenishRate()
                                : io.r2dbc.spi.Parameters.inOut(io.r2dbc.spi.R2dbcType.INTEGER))
                .bind("burstCapacity",
                        entity.getRateLimitBurstCapacity() != null
                                ? entity.getRateLimitBurstCapacity()
                                : io.r2dbc.spi.Parameters.inOut(io.r2dbc.spi.R2dbcType.INTEGER))
                .bind("requestedTokens",
                        entity.getRateLimitRequestedTokens() != null
                                ? entity.getRateLimitRequestedTokens()
                                : io.r2dbc.spi.Parameters.inOut(io.r2dbc.spi.R2dbcType.INTEGER))
                .fetch().rowsUpdated().then();
    }
}
