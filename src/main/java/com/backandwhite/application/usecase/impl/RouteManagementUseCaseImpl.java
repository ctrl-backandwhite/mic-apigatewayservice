package com.backandwhite.application.usecase.impl;

import com.backandwhite.application.usecase.RouteManagementUseCase;
import com.backandwhite.common.exception.EntityNotFoundException;
import com.backandwhite.common.exception.Message;
import com.backandwhite.domain.model.GatewayRoute;
import com.backandwhite.infrastructure.facade.GatewayRouteFacade;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Log4j2
@Service
@RequiredArgsConstructor
public class RouteManagementUseCaseImpl implements RouteManagementUseCase {

    private static final String ENTITY_NAME = "GatewayRoute";

    private final GatewayRouteFacade routeFacade;

    @Override
    public Flux<GatewayRoute> findAll() {
        return routeFacade.findAll();
    }

    @Override
    public Mono<GatewayRoute> findById(String id) {
        return routeFacade.findById(id)
                .switchIfEmpty(Mono.error(new EntityNotFoundException(Message.ENTITY_NOT_FOUND.getCode(),
                        Message.ENTITY_NOT_FOUND.format(ENTITY_NAME, id))));
    }

    @Override
    public Mono<GatewayRoute> create(GatewayRoute route) {
        return routeFacade.save(route);
    }

    @Override
    public Mono<GatewayRoute> update(GatewayRoute route, String id) {
        return routeFacade.findById(id)
                .switchIfEmpty(Mono.error(new EntityNotFoundException(Message.ENTITY_NOT_FOUND.getCode(),
                        Message.ENTITY_NOT_FOUND.format(ENTITY_NAME, id))))
                .flatMap(existing -> routeFacade.update(route, id));
    }

    @Override
    public Mono<Void> delete(String id) {
        return routeFacade.findById(id)
                .switchIfEmpty(Mono.error(new EntityNotFoundException(Message.ENTITY_NOT_FOUND.getCode(),
                        Message.ENTITY_NOT_FOUND.format(ENTITY_NAME, id))))
                .flatMap(existing -> routeFacade.delete(id));
    }

    @Override
    public Mono<GatewayRoute> toggleEnabled(String id) {
        return routeFacade.findById(id)
                .switchIfEmpty(Mono.error(new EntityNotFoundException(Message.ENTITY_NOT_FOUND.getCode(),
                        Message.ENTITY_NOT_FOUND.format(ENTITY_NAME, id))))
                .flatMap(existing -> routeFacade.toggleEnabled(id));
    }

    @Override
    public Mono<Long> bulkDelete(List<String> ids) {
        return Flux.fromIterable(ids)
                .flatMap(id -> routeFacade.delete(id).thenReturn(1L).onErrorResume(e -> Mono.just(0L)))
                .reduce(0L, Long::sum).doOnSuccess(deleted -> {
                    if (deleted > 0)
                        routeFacade.publishRefreshEvent();
                });
    }

    @Override
    @SuppressWarnings("java:S4030")
    public Mono<Map<String, Object>> bulkImport(List<GatewayRoute> routes) {
        List<String> skippedIds = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        return Flux.fromIterable(routes).concatMap(route -> routeFacade.findById(route.getId()).flatMap(existing -> {
            skippedIds.add(route.getId());
            return Mono.<GatewayRoute>empty();
        }).switchIfEmpty(routeFacade.save(route).onErrorResume(e -> {
            errors.add(route.getId() + ": " + e.getMessage());
            return Mono.empty();
        }))).count().map(created -> {
            if (created > 0)
                routeFacade.publishRefreshEvent();
            return Map.<String, Object>of("created", created.intValue(), "skipped", skippedIds.size(), "skippedIds",
                    List.copyOf(skippedIds), "errors", List.copyOf(errors), "errorCount", errors.size());
        });
    }

    @Override
    public Mono<Void> refresh() {
        return Mono.fromRunnable(routeFacade::publishRefreshEvent);
    }
}
