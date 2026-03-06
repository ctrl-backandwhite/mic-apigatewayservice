package com.backandwhite.infrastructure.seeder;

import com.backandwhite.domain.model.GatewayRoute;
import com.backandwhite.domain.repository.GatewayRouteRepository;
import com.backandwhite.infrastructure.configuration.ServicesProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Siembra las rutas iniciales de los 15 microservicios en PostgreSQL
 * solo si la tabla {@code gateway_route} está vacía (primer arranque).
 *
 * <p>En arranques posteriores las rutas se cargan directamente desde PostgreSQL.
 * Las URLs de los servicios se inyectan desde el perfil de configuración activo.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class RouteSeeder implements ApplicationRunner {

    private final GatewayRouteRepository routeRepository;
    private final ServicesProperties services;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void run(ApplicationArguments args) {
        routeRepository.count()
                .flatMap(count -> {
                    if (count == 0) {
                        log.info("Gateway route table is empty. Seeding initial routes...");
                        return seedRoutes();
                    }
                    log.info("Gateway routes already seeded ({} routes found). Skipping.", count);
                    return Mono.empty();
                })
                .block();
    }

    private Mono<Void> seedRoutes() {
        List<GatewayRoute> routes = buildInitialRoutes();
        return routeRepository.findAll()
                .collectList()
                .flatMap(existing -> {
                    if (!existing.isEmpty()) return Mono.empty();
                    return saveAll(routes);
                });
    }

    private Mono<Void> saveAll(List<GatewayRoute> routes) {
        return Flux.fromIterable(routes)
                .flatMap(routeRepository::save)
                .then()
                .doOnSuccess(v -> {
                    eventPublisher.publishEvent(new RefreshRoutesEvent(this));
                    log.info("Seeded {} initial gateway routes.", routes.size());
                });
    }

    private List<GatewayRoute> buildInitialRoutes() {
        return List.of(
                route("config-service", services.config().url(),
                        List.of("Path=/api/v1/config/**,/api/v1/languages/**,/api/v1/currencies/**,/api/v1/countries/**,/api/v1/zones/**,/api/v1/settings/**"), 0),

                route("iam-service", services.iam().url(),
                        List.of("Path=/api/v1/auth/**,/api/v1/employees/**,/api/v1/profiles/**,/api/v1/permissions/**,/api/v1/api-clients/**"), 0),

                route("catalog-service", services.catalog().url(),
                        List.of("Path=/api/v1/products/**,/api/v1/categories/**,/api/v1/manufacturers/**,/api/v1/suppliers/**,/api/v1/search/**"), 0),

                route("customer-service", services.customer().url(),
                        List.of("Path=/api/v1/customers/**"), 0),

                route("tax-service", services.tax().url(),
                        List.of("Path=/api/v1/taxes/**,/api/v1/tax-rules-groups/**"), 0),

                route("inventory-service", services.inventory().url(),
                        List.of("Path=/api/v1/stock/**,/api/v1/warehouses/**,/api/v1/supply-orders/**"), 0),

                route("pricing-service", services.pricing().url(),
                        List.of("Path=/api/v1/pricing/**,/api/v1/specific-prices/**,/api/v1/catalog-price-rules/**"), 0),

                route("shipping-service", services.shipping().url(),
                        List.of("Path=/api/v1/carriers/**,/api/v1/shipping/**"), 0),

                route("cart-service", services.cart().url(),
                        List.of("Path=/api/v1/carts/**"), 0),

                route("order-service", services.order().url(),
                        List.of("Path=/api/v1/orders/**,/api/v1/order-states/**"), 0),

                route("payment-service", services.payment().url(),
                        List.of("Path=/api/v1/payments/**,/api/v1/payment-methods/**"), 0),

                route("notification-service", services.notification().url(),
                        List.of("Path=/api/v1/notifications/**"), 0),

                route("cms-service", services.cms().url(),
                        List.of("Path=/api/v1/pages/**,/api/v1/seo/**,/api/v1/stores/**,/api/v1/contact/**"), 0),

                route("media-service", services.media().url(),
                        List.of("Path=/api/v1/images/**,/api/v1/attachments/**"), 0),

                route("analytics-service", services.analytics().url(),
                        List.of("Path=/api/v1/analytics/**"), 0),

                // Ruta para la API de gestión del propio gateway (siempre presente)
                route("gateway-management", "forward:///",
                        List.of("Path=/api/v1/gateway/**"), -1)
        );
    }

    private GatewayRoute route(String id, String uri, List<String> predicates, int order) {
        return GatewayRoute.builder()
                .id(id)
                .uri(uri)
                .predicates(predicates)
                .filters(List.of())
                .order(order)
                .enabled(true)
                .build();
    }
}
