package com.backandwhite.application.usecase.impl;

import com.backandwhite.application.usecase.RouteManagementUseCase;
import com.backandwhite.common.exception.EntityNotFoundException;
import com.backandwhite.common.exception.Message;
import com.backandwhite.domain.model.GatewayRoute;
import com.backandwhite.domain.repository.GatewayRouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Log4j2
@Service
@RequiredArgsConstructor
public class RouteManagementUseCaseImpl implements RouteManagementUseCase {

    private static final String ENTITY_NAME = "GatewayRoute";

    private final GatewayRouteRepository routeRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Flux<GatewayRoute> findAll() {
        return routeRepository.findAll();
    }

    @Override
    public Mono<GatewayRoute> findById(String id) {
        return routeRepository.findById(id)
                .switchIfEmpty(Mono.error(new EntityNotFoundException(
                        Message.ENTITY_NOT_FOUND.getCode(),
                        Message.ENTITY_NOT_FOUND.format(ENTITY_NAME, id)
                )));
    }

    @Override
    public Mono<GatewayRoute> create(GatewayRoute route) {
        return routeRepository.save(route)
                .doOnSuccess(saved -> publishRefreshEvent());
    }

    @Override
    public Mono<GatewayRoute> update(GatewayRoute route, String id) {
        return routeRepository.findById(id)
                .switchIfEmpty(Mono.error(new EntityNotFoundException(
                        Message.ENTITY_NOT_FOUND.getCode(),
                        Message.ENTITY_NOT_FOUND.format(ENTITY_NAME, id)
                )))
                .flatMap(existing -> routeRepository.update(route, id))
                .doOnSuccess(updated -> publishRefreshEvent());
    }

    @Override
    public Mono<Void> delete(String id) {
        return routeRepository.findById(id)
                .switchIfEmpty(Mono.error(new EntityNotFoundException(
                        Message.ENTITY_NOT_FOUND.getCode(),
                        Message.ENTITY_NOT_FOUND.format(ENTITY_NAME, id)
                )))
                .flatMap(existing -> routeRepository.delete(id))
                .doOnSuccess(v -> publishRefreshEvent());
    }

    @Override
    public Mono<GatewayRoute> toggleEnabled(String id) {
        return routeRepository.findById(id)
                .switchIfEmpty(Mono.error(new EntityNotFoundException(
                        Message.ENTITY_NOT_FOUND.getCode(),
                        Message.ENTITY_NOT_FOUND.format(ENTITY_NAME, id)
                )))
                .flatMap(existing -> routeRepository.toggleEnabled(id))
                .doOnSuccess(updated -> publishRefreshEvent());
    }

    @Override
    public Mono<Void> refresh() {
        return Mono.fromRunnable(this::publishRefreshEvent);
    }

    private void publishRefreshEvent() {
        eventPublisher.publishEvent(new RefreshRoutesEvent(this));
        log.info("Gateway routes refresh event published.");
    }
}
