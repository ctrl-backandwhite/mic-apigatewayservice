package com.backandwhite.application.usecase;

import com.backandwhite.domain.model.GatewayRoute;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

    Mono<Long> bulkDelete(List<String> ids);

    /**
     * Importa una lista de rutas masivamente. Las rutas con id duplicado se
     * omiten y se reportan en el resultado.
     */
    Mono<Map<String, Object>> bulkImport(List<GatewayRoute> routes);
    Mono<GatewayRoute> toggleEnabled(String id);

    /**
     * Publica el evento de refresco de rutas en el contexto del gateway
     * para que Spring Cloud Gateway recargue las definiciones desde PostgreSQL
     * sin necesidad de reiniciar el servicio.
     */
    Mono<Void> refresh();
}
