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

import java.time.Duration;
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
                                .timeout(Duration.ofSeconds(8))
                                .flatMap(count -> {
                                        if (count == 0) {
                                                log.info("Gateway route table is empty. Seeding initial routes...");
                                                return seedRoutes();
                                        }
                                        log.info("Gateway routes already seeded ({} routes found). Skipping.", count);
                                        return Mono.empty();
                                })
                                .onErrorResume(error -> {
                                        log.error(
                                                        "Unable to seed gateway routes at startup. Continuing without seeding. Cause: {}",
                                                        error.getMessage(),
                                                        error);
                                        return Mono.empty();
                                })
                                .subscribe();
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

                addRouteIfConfigured(routes, "auth-service", services.auth(),
                                List.of(
                                                "Path=/api/v1/auth/**",
                                                "Path=/api/v1/users/**",
                                                "Path=/api/v1/roles/**",
                                                "Path=/api/v1/groups/**",
                                                "Path=/api/v1/scopes/**",
                                                "Path=/api/v1/granttypes/**",
                                                "Path=/api/v1/redirecturis/**",
                                                "Path=/api/v1/oauthclients/**"),
                                0, 10, 20, 1);

                addRouteIfConfigured(routes, "notification-service", services.notification(),
                                List.of(
                                                "Path=/api/v1/notifications/**",
                                                "Path=/api/v1/notification-templates/**"),
                                0, 10, 20, 1);

                addRouteIfConfigured(routes, "catalog-service", services.catalog(),
                                List.of(
                                                "Path=/api/v1/categories/**",
                                                "Path=/api/v1/products/**"),
                                0, 10, 20, 1);
                routes.add(GatewayRoute.builder()
                                .id("gateway-management")
                                .uri("forward:///")
                                .predicates(List.of("Path=/api/v1/gateway/**"))
                                .filters(List.of())
                                .order(-1)
                                .enabled(true)
                                .build());

                return routes;
        }

        private void addRouteIfConfigured(
                        List<GatewayRoute> routes,
                        String routeId,
                        ServicesProperties.Service service,
                        List<String> predicates,
                        int order,
                        int replenishRate,
                        int burstCapacity,
                        int requestedTokens) {
                if (service == null || service.url() == null || service.url().isBlank()) {
                        log.warn("Skipping seed route '{}' because service URL is not configured.", routeId);
                        return;
                }

                routes.add(route(routeId, service.url(), predicates, order,
                                replenishRate, burstCapacity, requestedTokens));
        }

        private GatewayRoute route(String id, String uri, List<String> predicates, int order,
                        int replenishRate, int burstCapacity, int requestedTokens) {
                return GatewayRoute.builder()
                                .id(id)
                                .uri(uri)
                                .predicates(predicates)
                                .filters(List.of())
                                .order(order)
                                .enabled(true)
                                .rateLimitReplenishRate(replenishRate)
                                .rateLimitBurstCapacity(burstCapacity)
                                .rateLimitRequestedTokens(requestedTokens)
                                .build();
        }
}
