package com.backandwhite.application.usecase;

import com.backandwhite.domain.model.GatewayRoute;
import java.util.List;
import java.util.Map;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Use cases for dynamic route management in the API Gateway. Allows
 * registering, updating and deleting routes without redeploying.
 */
public interface RouteManagementUseCase {

    Flux<GatewayRoute> findAll();

    Mono<GatewayRoute> findById(String id);

    Mono<GatewayRoute> create(GatewayRoute route);

    Mono<GatewayRoute> update(GatewayRoute route, String id);

    Mono<Void> delete(String id);

    Mono<Long> bulkDelete(List<String> ids);

    /**
     * Bulk-imports a list of routes. Routes with duplicate ids are skipped and
     * reported in the result.
     */
    Mono<Map<String, Object>> bulkImport(List<GatewayRoute> routes);

    Mono<GatewayRoute> toggleEnabled(String id);

    /**
     * Publishes a route refresh event in the gateway context so that Spring Cloud
     * Gateway reloads definitions from PostgreSQL without restarting the service.
     */
    Mono<Void> refresh();
}
