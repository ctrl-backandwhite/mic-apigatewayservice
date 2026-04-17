package com.backandwhite.provider.route;

import com.backandwhite.api.dto.in.RouteDefinitionDtoIn;
import com.backandwhite.domain.model.GatewayRoute;
import com.backandwhite.provider.BaseProvider;
import java.time.LocalDateTime;
import java.util.List;

public final class RouteDefinitionProvider extends BaseProvider {

    public static final String ROUTE_ID_ONE = "catalog-service";
    public static final String ROUTE_ID_TWO = "order-service";

    private RouteDefinitionProvider() {
    }

    public static GatewayRoute getCatalogRoute() {
        return GatewayRoute.builder().id(ROUTE_ID_ONE).uri("http://localhost:8083")
                .predicates(List.of("Path=/api/v1/products/**,/api/v1/categories/**")).filters(List.of()).order(0)
                .enabled(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    public static GatewayRoute getOrderRoute() {
        return GatewayRoute.builder().id(ROUTE_ID_TWO).uri("http://localhost:8090")
                .predicates(List.of("Path=/api/v1/orders/**")).filters(List.of()).order(0).enabled(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    public static RouteDefinitionDtoIn getRouteDefinitionDtoInOne() {
        return RouteDefinitionDtoIn.builder().id(ROUTE_ID_ONE).uri("http://localhost:8083")
                .predicates(List.of("Path=/api/v1/products/**")).filters(List.of()).order(0).build();
    }

    public static RouteDefinitionDtoIn getRouteDefinitionDtoInTwo() {
        return RouteDefinitionDtoIn.builder().id(ROUTE_ID_TWO).uri("http://localhost:8090")
                .predicates(List.of("Path=/api/v1/orders/**")).filters(List.of()).order(0).build();
    }
}
