package com.backandwhite.infrastructure.repository.impl;

import com.backandwhite.domain.model.GatewayRoute;
import com.backandwhite.domain.repository.GatewayRouteRepository;
import com.backandwhite.infrastructure.entity.GatewayRouteEntity;
import com.backandwhite.infrastructure.mapper.GatewayRouteEntityMapper;
import com.backandwhite.infrastructure.repository.GatewayRouteR2dbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class GatewayRouteRepositoryImpl implements GatewayRouteRepository {

    private final GatewayRouteR2dbcRepository r2dbcRepository;
    private final GatewayRouteEntityMapper entityMapper;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;

    @Override
    public Flux<GatewayRoute> findAll() {
        return r2dbcRepository.findAll()
                .map(entityMapper::toDomain);
    }

    @Override
    public Flux<GatewayRoute> findAllEnabled() {
        return r2dbcRepository.findByEnabledTrue()
                .map(entityMapper::toDomain);
    }

    @Override
    public Mono<GatewayRoute> findById(String id) {
        return r2dbcRepository.findById(id)
                .map(entityMapper::toDomain);
    }

    @Override
    public Mono<GatewayRoute> save(GatewayRoute route) {
        GatewayRouteEntity entity = entityMapper.toEntity(route);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return r2dbcEntityTemplate.insert(entity)
                .map(entityMapper::toDomain);
    }

    @Override
    public Mono<GatewayRoute> update(GatewayRoute route, String id) {
        return r2dbcRepository.findById(id)
                .flatMap(existing -> {
                    GatewayRouteEntity updated = entityMapper.toEntity(route);
                    updated.setId(id);
                    updated.setCreatedAt(existing.getCreatedAt());
                    updated.setUpdatedAt(LocalDateTime.now());
                    return r2dbcRepository.save(updated);
                })
                .map(entityMapper::toDomain);
    }

    @Override
    public Mono<Void> delete(String id) {
        return r2dbcRepository.deleteById(id);
    }

    @Override
    public Mono<GatewayRoute> toggleEnabled(String id) {
        return r2dbcRepository.findById(id)
                .flatMap(entity -> {
                    entity.setEnabled(!entity.isEnabled());
                    entity.setUpdatedAt(LocalDateTime.now());
                    return r2dbcRepository.save(entity);
                })
                .map(entityMapper::toDomain);
    }

    @Override
    public Mono<Long> count() {
        return r2dbcRepository.count();
    }
}
