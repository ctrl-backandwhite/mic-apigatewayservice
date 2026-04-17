package com.backandwhite.infrastructure.facade;

import com.backandwhite.domain.model.GatewayRoute;
import com.backandwhite.domain.repository.GatewayRouteRepository;
import com.backandwhite.infrastructure.mapper.GatewayRouteEntityMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Facade that encapsulates the complexity of gateway route management.
 *
 * <p>
 * Coordinates access to the route repository, publication of refresh events and
 * mapping between layers. Use cases delegate to this facade to avoid coupling
 * directly to infrastructure.
 *
 * <p>
 * Facade pattern applied:
 * <ul>
 * <li>{@link GatewayRouteRepository} — reactive persistence</li>
 * <li>{@link ApplicationEventPublisher} — gateway refresh events</li>
 * <li>{@link GatewayRouteEntityMapper} — entity↔domain conversion</li>
 * </ul>
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class GatewayRouteFacade {

    private final GatewayRouteRepository routeRepository;
    private final ApplicationEventPublisher eventPublisher;

    public Flux<GatewayRoute> findAll() {
        return routeRepository.findAll();
    }

    public Mono<GatewayRoute> findById(String id) {
        return routeRepository.findById(id);
    }

    public Mono<GatewayRoute> save(GatewayRoute route) {
        return routeRepository.save(route).doOnSuccess(saved -> publishRefreshEvent());
    }

    public Mono<GatewayRoute> update(GatewayRoute route, String id) {
        return routeRepository.update(route, id).doOnSuccess(updated -> publishRefreshEvent());
    }

    public Mono<Void> delete(String id) {
        return routeRepository.delete(id).doOnSuccess(v -> publishRefreshEvent());
    }

    public Mono<GatewayRoute> toggleEnabled(String id) {
        return routeRepository.toggleEnabled(id).doOnSuccess(toggled -> publishRefreshEvent());
    }

    public Mono<Void> upsert(GatewayRoute route) {
        return routeRepository.upsert(route);
    }

    public Mono<Void> upsertAll(List<GatewayRoute> routes) {
        return Flux.fromIterable(routes).concatMap(routeRepository::upsert).then().doOnSuccess(v -> {
            publishRefreshEvent();
            log.info("Gateway routes synced ({} routes).", routes.size());
        });
    }

    public void publishRefreshEvent() {
        eventPublisher.publishEvent(new RefreshRoutesEvent(this));
        log.info("Gateway routes refresh event published.");
    }
}
