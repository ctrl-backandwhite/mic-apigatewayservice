package com.backandwhite.application.usecase;

import com.backandwhite.domain.model.GatewayRoute;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Casos de uso para la gestión dinámica de rutas del API Gateway.
 * Permite registrar, actualizar y eliminar rutas sin necesidad de redeploy.
 */
public interface RouteManagementUseCase {

    Flux<GatewayRoute> findAll();

    Mono<GatewayRoute> findById(String id);

    Mono<GatewayRoute> create(GatewayRoute route);

    Mono<GatewayRoute> update(GatewayRoute route, String id);

    Mono<Void> delete(String id);

    Mono<Long> bulkDelete(List<String> ids);

    Mono<GatewayRoute> toggleEnabled(String id);

    /**
     * Publica el evento de refresco de rutas en el contexto del gateway
     * para que Spring Cloud Gateway recargue las definiciones desde PostgreSQL
     * sin necesidad de reiniciar el servicio.
     */
    Mono<Void> refresh();
}
