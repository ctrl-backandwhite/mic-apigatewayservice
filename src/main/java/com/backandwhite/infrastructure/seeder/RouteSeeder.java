package com.backandwhite.infrastructure.seeder;

import com.backandwhite.domain.model.GatewayRoute;
import com.backandwhite.domain.repository.GatewayRouteRepository;
import com.backandwhite.infrastructure.configuration.ServicesProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Siembra las rutas iniciales de los 15 microservicios en PostgreSQL
 * solo si la tabla {@code gateway_route} está vacía (primer arranque).
 *
 * <p>
 * En arranques posteriores las rutas se cargan directamente desde PostgreSQL.
 * Las URLs de los servicios se inyectan desde el perfil de configuración
 * activo.
 */
@Log4j2
@Component
@DependsOnDatabaseInitialization
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
                                        if (!existing.isEmpty())
                                                return Mono.empty();
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
                List<GatewayRoute> routes = new ArrayList<>();

                addRouteIfConfigured(routes, "config-service", services.config(),
                                List.of("Path=/api/v1/config/**,/api/v1/languages/**,/api/v1/currencies/**,/api/v1/countries/**,/api/v1/zones/**,/api/v1/settings/**"),
                                0);

                addRouteIfConfigured(routes, "iam-service", services.iam(),
                                List.of("Path=/api/v1/auth/**,/api/v1/employees/**,/api/v1/profiles/**,/api/v1/permissions/**,/api/v1/api-clients/**"),
                                0);

                addRouteIfConfigured(routes, "catalog-service", services.catalog(),
                                List.of("Path=/api/v1/products/**,/api/v1/categories/**,/api/v1/manufacturers/**,/api/v1/suppliers/**,/api/v1/search/**"),
                                0);

                addRouteIfConfigured(routes, "customer-service", services.customer(),
                                List.of("Path=/api/v1/customers/**"), 0);

                addRouteIfConfigured(routes, "tax-service", services.tax(),
                                List.of("Path=/api/v1/taxes/**,/api/v1/tax-rules-groups/**"), 0);

                addRouteIfConfigured(routes, "inventory-service", services.inventory(),
                                List.of("Path=/api/v1/stock/**,/api/v1/warehouses/**,/api/v1/supply-orders/**"),
                                0);

                addRouteIfConfigured(routes, "pricing-service", services.pricing(),
                                List.of("Path=/api/v1/pricing/**,/api/v1/specific-prices/**,/api/v1/catalog-price-rules/**"),
                                0);

                addRouteIfConfigured(routes, "shipping-service", services.shipping(),
                                List.of("Path=/api/v1/carriers/**,/api/v1/shipping/**"), 0);

                addRouteIfConfigured(routes, "cart-service", services.cart(),
                                List.of("Path=/api/v1/carts/**"), 0);

                addRouteIfConfigured(routes, "order-service", services.order(),
                                List.of("Path=/api/v1/orders/**,/api/v1/order-states/**"), 0);

                addRouteIfConfigured(routes, "payment-service", services.payment(),
                                List.of("Path=/api/v1/payments/**,/api/v1/payment-methods/**"), 0);

                addRouteIfConfigured(routes, "notification-service", services.notification(),
                                List.of("Path=/api/v1/notifications/**"), 0);

                addRouteIfConfigured(routes, "cms-service", services.cms(),
                                List.of("Path=/api/v1/pages/**,/api/v1/seo/**,/api/v1/stores/**,/api/v1/contact/**"),
                                0);

                addRouteIfConfigured(routes, "media-service", services.media(),
                                List.of("Path=/api/v1/images/**,/api/v1/attachments/**"), 0);

                addRouteIfConfigured(routes, "analytics-service", services.analytics(),
                                List.of("Path=/api/v1/analytics/**"), 0);

                routes.add(route("gateway-management", "forward:///",
                                List.of("Path=/api/v1/gateway/**"), -1));

                return routes;
        }

        private void addRouteIfConfigured(
                        List<GatewayRoute> routes,
                        String routeId,
                        ServicesProperties.Service service,
                        List<String> predicates,
                        int order) {
                if (service == null || service.url() == null || service.url().isBlank()) {
                        log.warn("Skipping seed route '{}' because service URL is not configured.", routeId);
                        return;
                }

                routes.add(route(routeId, service.url(), predicates, order));
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
