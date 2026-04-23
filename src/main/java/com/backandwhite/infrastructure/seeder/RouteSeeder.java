package com.backandwhite.infrastructure.seeder;

import com.backandwhite.domain.model.GatewayRoute;
import com.backandwhite.infrastructure.configuration.ServicesProperties;
import com.backandwhite.infrastructure.facade.GatewayRouteFacade;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Synchronizes routes defined in code with the {@code gateway_route} table on
 * each service startup.
 *
 * <p>
 * Uses upsert (INSERT ... ON CONFLICT DO UPDATE) so that:
 * <ul>
 * <li>On the first startup it creates all routes.</li>
 * <li>On subsequent startups it corrects predicates, uri or rate-limit if they
 * changed in code, without touching the {@code enabled} flag (operators can
 * disable a route manually and it will not be reset).</li>
 * <li>Extra routes created via the API remain untouched.</li>
 * </ul>
 *
 * <p>
 * Service URLs are injected from the active configuration profile via
 * {@link ServicesProperties}.
 */
@Log4j2
@Component
@DependsOnDatabaseInitialization
@RequiredArgsConstructor
public class RouteSeeder implements ApplicationRunner {

    private final GatewayRouteFacade routeFacade;
    private final ServicesProperties services;

    @Override
    public void run(ApplicationArguments args) {
        syncRoutes().onErrorResume(error -> {
            log.error("Unable to sync gateway routes at startup. Continuing. Cause: {}", error.getMessage(), error);
            return Mono.empty();
        }).subscribe();
    }

    /**
     * Upserts all defined routes and publishes a RefreshRoutesEvent so that the
     * gateway reloads them on the fly.
     */
    private Mono<Void> syncRoutes() {
        return routeFacade.upsertAll(buildRoutes());
    }

    private static final RateLimit NO_RATE_LIMIT = new RateLimit(0, 0, 0);
    private static final RateLimit STANDARD_RATE_LIMIT = new RateLimit(10, 20, 1);

    private List<GatewayRoute> buildRoutes() {
        List<GatewayRoute> routes = new ArrayList<>();

        addRouteIfConfigured(routes, "auth-service-oauth2", services.auth(),
                List.of("Path=/oauth2/**," + "/.well-known/**," + "/login,/login/**,/logout," + "/login.html,"
                        + "/register.html," + "/forgot-password.html," + "/reset-password.html,"
                        + "/reset-success.html," + "/reset-error.html," + "/activation-success.html,"
                        + "/activation-error.html," + "/terms.html," + "/css/**," + "/js/**," + "/images/**,"
                        + "/favicon.ico"),
                List.of(), -1, NO_RATE_LIMIT);

        addRouteIfConfigured(routes, "auth-service", services.auth(),
                List.of("Path=/api/v1/auth/**," + "/api/v1/users/**," + "/api/v1/roles/**," + "/api/v1/groups/**,"
                        + "/api/v1/permissions/**," + "/api/v1/scopes/**," + "/api/v1/granttypes/**,"
                        + "/api/v1/redirecturis/**," + "/api/v1/oauthclients/**"),
                0, STANDARD_RATE_LIMIT);

        addRouteIfConfigured(routes, "webapp", services.webapp(), List.of("Path=/nexa-auth/**"), List.of(), -2,
                NO_RATE_LIMIT);

        addRouteIfConfigured(routes, "notification-service", services.notification(),
                List.of("Path=/api/v1/notifications/**," + "/api/v1/notification-templates/**"), 0,
                STANDARD_RATE_LIMIT);

        addRouteIfConfigured(routes, "catalog-service", services.catalog(), List.of(
                "Path=/api/v1/categories/**,/api/v1/products/**,/api/v1/reviews/**,/api/v1/attributes/**,/api/v1/brands/**,/api/v1/media/**,/api/v1/public/**,/api/v1/warranties/**,/api/v1/price-rules/**,/api/v1/taxes/**,/api/v1/sync/**"),
                0, STANDARD_RATE_LIMIT);

        addRouteIfConfigured(routes, "cms-service", services.cms(),
                List.of("Path=/api/v1/currency-rates/**," + "/api/v1/settings/**," + "/api/v1/campaigns/**,"
                        + "/api/v1/slides/**," + "/api/v1/gift-cards/**," + "/api/v1/newsletter/**,"
                        + "/api/v1/loyalty/**," + "/api/v1/email-templates/**," + "/api/v1/seo/**,"
                        + "/api/v1/flows/**," + "/api/v1/contact/**"),
                0, STANDARD_RATE_LIMIT);

        addRouteIfConfigured(routes, "orders-cj-webhook", services.orders(), List.of("Path=/api/v1/cj/webhook/**"),
                List.of(), -2, NO_RATE_LIMIT);

        addRouteIfConfigured(routes, "orders-service", services.orders(),
                List.of("Path=/api/v1/orders/**," + "/api/v1/cart/**," + "/api/v1/tracking/**," + "/api/v1/shipping/**,"
                        + "/api/v1/returns/**," + "/api/v1/invoices/**," + "/api/v1/coupons/**,"
                        + "/api/v1/admin/cj-orders/**"),
                0, STANDARD_RATE_LIMIT);

        addRouteIfConfigured(routes, "payments-service", services.payments(), List.of("Path=/api/v1/payments/**"), 0,
                STANDARD_RATE_LIMIT);

        addRouteIfConfigured(routes, "userdetail-service", services.userdetail(),
                List.of("Path=/api/v1/profile/**," + "/api/v1/addresses/**," + "/api/v1/favorites/**,"
                        + "/api/v1/payment-methods/**," + "/api/v1/notification-prefs/**," + "/api/v1/customers/**"),
                0, STANDARD_RATE_LIMIT);

        routes.add(GatewayRoute.builder().id("gateway-management").uri("forward:///")
                .predicates(List.of("Path=/api/v1/gateway/**")).filters(List.of()).order(-1).enabled(true).build());

        addRouteIfConfigured(routes, "ecomerce-frontend", services.ecommerce(), List.of("Path=/**"), 100,
                NO_RATE_LIMIT);

        return routes;
    }

    private void addRouteIfConfigured(List<GatewayRoute> routes, String routeId, ServicesProperties.Service service,
            List<String> predicates, int order, RateLimit rateLimit) {
        addRouteIfConfigured(routes, routeId, service, predicates, List.of(), order, rateLimit);
    }

    private void addRouteIfConfigured(List<GatewayRoute> routes, String routeId, ServicesProperties.Service service,
            List<String> predicates, List<String> filters, int order, RateLimit rateLimit) {
        if (service == null || service.url() == null || service.url().isBlank()) {
            log.warn("Skipping route '{}' — service URL not configured.", routeId);
            return;
        }
        routes.add(route(routeId, service.url().strip(), predicates, filters, order, rateLimit));
    }

    private GatewayRoute route(String id, String uri, List<String> predicates, List<String> filters, int order,
            RateLimit rateLimit) {
        boolean hasRateLimit = rateLimit.replenishRate() > 0;
        return GatewayRoute.builder().id(id).uri(uri).predicates(predicates).filters(filters).order(order).enabled(true)
                .rateLimitReplenishRate(hasRateLimit ? rateLimit.replenishRate() : null)
                .rateLimitBurstCapacity(hasRateLimit ? rateLimit.burstCapacity() : null)
                .rateLimitRequestedTokens(hasRateLimit ? rateLimit.requestedTokens() : null).build();
    }

    private record RateLimit(int replenishRate, int burstCapacity, int requestedTokens) {
    }
}
