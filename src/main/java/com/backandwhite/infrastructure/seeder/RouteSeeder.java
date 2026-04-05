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
 * Sincroniza las rutas definidas en el código con la tabla
 * {@code gateway_route}
 * en cada arranque del servicio.
 *
 * <p>
 * Usa upsert (INSERT ... ON CONFLICT DO UPDATE) por lo que:
 * <ul>
 * <li>En el primer arranque crea todas las rutas.</li>
 * <li>En arranques posteriores corrige predicates, uri o rate-limit si
 * cambiaron en el código, sin tocar el flag {@code enabled} (el operador
 * puede deshabilitar una ruta manualmente y no se resetea).</li>
 * <li>Rutas extra creadas vía API quedan intactas.</li>
 * </ul>
 *
 * <p>
 * Las URLs de los servicios se inyectan desde el perfil de configuración activo
 * a través de {@link ServicesProperties}.
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
                syncRoutes()
                                .onErrorResume(error -> {
                                        log.error(
                                                        "Unable to sync gateway routes at startup. Continuing. Cause: {}",
                                                        error.getMessage(),
                                                        error);
                                        return Mono.empty();
                                })
                                .subscribe();
        }

        /**
         * Hace upsert de todas las rutas definidas y publica RefreshRoutesEvent
         * para que el gateway las recargue en caliente.
         */
        private Mono<Void> syncRoutes() {
                List<GatewayRoute> routes = buildRoutes();
                if (routes.isEmpty()) {
                        log.warn("No service URLs configured — skipping route sync.");
                        return Mono.empty();
                }
                return Flux.fromIterable(routes)
                                .concatMap(routeRepository::upsert)
                                .then()
                                .doOnSuccess(v -> {
                                        eventPublisher.publishEvent(new RefreshRoutesEvent(this));
                                        log.info("Gateway routes synced ({} routes).", routes.size());
                                });
        }

        private List<GatewayRoute> buildRoutes() {
                List<GatewayRoute> routes = new ArrayList<>();

                // Un único Path= con patrones separados por coma → PathRoutePredicateFactory
                // hace OR interno. Múltiples Path= separados serían AND → nunca coinciden.
                addRouteIfConfigured(routes, "auth-service-oauth2", services.auth(),
                                List.of("Path=/oauth2/**," +
                                                "/.well-known/**," +
                                                "/login,/logout," +
                                                "/login.html," +
                                                "/register.html," +
                                                "/forgot-password.html," +
                                                "/reset-password.html," +
                                                "/reset-success.html," +
                                                "/reset-error.html," +
                                                "/activation-success.html," +
                                                "/activation-error.html," +
                                                "/terms.html," +
                                                "/css/**," +
                                                "/js/**," +
                                                "/images/**," +
                                                "/favicon.ico"),
                                List.of("RewriteLocationResponseHeader=AS_IN_REQUEST, Location, ,"),
                                -1, 0, 0, 0);

                addRouteIfConfigured(routes, "auth-service", services.auth(),
                                List.of("Path=/api/v1/auth/**," +
                                                "/api/v1/users/**," +
                                                "/api/v1/roles/**," +
                                                "/api/v1/groups/**," +
                                                "/api/v1/permissions/**," +
                                                "/api/v1/scopes/**," +
                                                "/api/v1/granttypes/**," +
                                                "/api/v1/redirecturis/**," +
                                                "/api/v1/oauthclients/**"),
                                0, 10, 20, 1);

                addRouteIfConfigured(routes, "webapp", services.webapp(),
                                List.of("Path=/nexa-auth/**"),
                                List.of(),
                                -2, 0, 0, 0);

                addRouteIfConfigured(routes, "notification-service", services.notification(),
                                List.of("Path=/api/v1/notifications/**," +
                                                "/api/v1/notification-templates/**"),
                                0, 10, 20, 1);

                addRouteIfConfigured(routes, "catalog-service", services.catalog(),
                                List.of("Path=/api/v1/categories/**,/api/v1/products/**,/api/v1/reviews/**"),
                                0, 10, 20, 1);

                routes.add(GatewayRoute.builder()
                                .id("gateway-management")
                                .uri("forward:///")
                                .predicates(List.of("Path=/api/v1/gateway/**"))
                                .filters(List.of())
                                .order(-1)
                                .enabled(true)
                                .build());

                // Catch-all: ecommerce SPA — must be last (highest order number)
                addRouteIfConfigured(routes, "ecomerce-frontend", services.ecommerce(),
                                List.of("Path=/**"),
                                100, 0, 0, 0);

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
                addRouteIfConfigured(routes, routeId, service, predicates, List.of(), order,
                                replenishRate, burstCapacity, requestedTokens);
        }

        private void addRouteIfConfigured(
                        List<GatewayRoute> routes,
                        String routeId,
                        ServicesProperties.Service service,
                        List<String> predicates,
                        List<String> filters,
                        int order,
                        int replenishRate,
                        int burstCapacity,
                        int requestedTokens) {
                if (service == null || service.url() == null || service.url().isBlank()) {
                        log.warn("Skipping route '{}' — service URL not configured.", routeId);
                        return;
                }
                String url = service.url().strip();
                if (url.isBlank()) {
                        log.warn("Skipping route '{}' — service URL is blank after stripping whitespace.", routeId);
                        return;
                }
                routes.add(route(routeId, url, predicates, filters, order,
                                replenishRate, burstCapacity, requestedTokens));
        }

        private GatewayRoute route(String id, String uri, List<String> predicates, List<String> filters,
                        int order, int replenishRate, int burstCapacity, int requestedTokens) {
                boolean hasRateLimit = replenishRate > 0;
                return GatewayRoute.builder()
                                .id(id)
                                .uri(uri)
                                .predicates(predicates)
                                .filters(filters)
                                .order(order)
                                .enabled(true)
                                .rateLimitReplenishRate(hasRateLimit ? replenishRate : null)
                                .rateLimitBurstCapacity(hasRateLimit ? burstCapacity : null)
                                .rateLimitRequestedTokens(hasRateLimit ? requestedTokens : null)
                                .build();
        }
}
