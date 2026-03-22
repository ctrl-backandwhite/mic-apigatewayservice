package com.backandwhite.infrastructure.repository;

import com.backandwhite.domain.model.GatewayRoute;
import com.backandwhite.domain.repository.GatewayRouteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostgresRouteDefinitionRepositoryTest {

    @Mock
    private GatewayRouteRepository routeRepository;

    @InjectMocks
    private PostgresRouteDefinitionRepository repository;

    @Test
    void getRouteDefinitions_withRateLimit_injectsRequestRateLimiterFilter() {
        GatewayRoute route = GatewayRoute.builder()
                .id("catalog-service")
                .uri("http://localhost:8083")
                .predicates(List.of("Path=/api/v1/products/**"))
                .filters(List.of())
                .order(0)
                .enabled(true)
                .rateLimitReplenishRate(20)
                .rateLimitBurstCapacity(40)
                .rateLimitRequestedTokens(1)
                .build();

        when(routeRepository.findAllEnabled()).thenReturn(Flux.just(route));

        StepVerifier.create(repository.getRouteDefinitions())
                .assertNext(def -> {
                    assertThat(def.getId()).isEqualTo("catalog-service");
                    assertThat(def.getFilters()).hasSize(1);

                    FilterDefinition rl = def.getFilters().getFirst();
                    assertThat(rl.getName()).isEqualTo("RequestRateLimiter");
                    assertThat(rl.getArgs())
                            .containsEntry("redis-rate-limiter.replenishRate", "20")
                            .containsEntry("redis-rate-limiter.burstCapacity", "40")
                            .containsEntry("redis-rate-limiter.requestedTokens", "1")
                            .containsEntry("key-resolver", "#{@ipKeyResolver}")
                            .containsEntry("deny-empty-key", "true");
                })
                .verifyComplete();
    }

    @Test
    void getRouteDefinitions_withoutRateLimit_noRateLimiterFilter() {
        GatewayRoute route = GatewayRoute.builder()
                .id("gateway-management")
                .uri("forward:///")
                .predicates(List.of("Path=/api/v1/gateway/**"))
                .filters(List.of())
                .order(-1)
                .enabled(true)
                .build();

        when(routeRepository.findAllEnabled()).thenReturn(Flux.just(route));

        StepVerifier.create(repository.getRouteDefinitions())
                .assertNext(def -> {
                    assertThat(def.getId()).isEqualTo("gateway-management");
                    assertThat(def.getFilters()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    void getRouteDefinitions_withRateLimitAndExplicitFilters_mergesBoth() {
        GatewayRoute route = GatewayRoute.builder()
                .id("catalog-service")
                .uri("http://localhost:8083")
                .predicates(List.of("Path=/api/v1/products/**"))
                .filters(List.of("StripPrefix=1"))
                .order(0)
                .enabled(true)
                .rateLimitReplenishRate(10)
                .rateLimitBurstCapacity(20)
                .rateLimitRequestedTokens(2)
                .build();

        when(routeRepository.findAllEnabled()).thenReturn(Flux.just(route));

        StepVerifier.create(repository.getRouteDefinitions())
                .assertNext(def -> {
                    assertThat(def.getFilters()).hasSize(2);

                    FilterDefinition stripPrefix = def.getFilters().get(0);
                    assertThat(stripPrefix.getName()).isEqualTo("StripPrefix");

                    FilterDefinition rl = def.getFilters().get(1);
                    assertThat(rl.getName()).isEqualTo("RequestRateLimiter");
                    assertThat(rl.getArgs())
                            .containsEntry("redis-rate-limiter.replenishRate", "10")
                            .containsEntry("redis-rate-limiter.burstCapacity", "20")
                            .containsEntry("redis-rate-limiter.requestedTokens", "2");
                })
                .verifyComplete();
    }

    @Test
    void getRouteDefinitions_withDefaultRequestedTokens_defaultsToOne() {
        GatewayRoute route = GatewayRoute.builder()
                .id("payment-service")
                .uri("http://localhost:8091")
                .predicates(List.of("Path=/api/v1/payments/**"))
                .filters(List.of())
                .order(0)
                .enabled(true)
                .rateLimitReplenishRate(5)
                .rateLimitBurstCapacity(10)
                // rateLimitRequestedTokens is null
                .build();

        when(routeRepository.findAllEnabled()).thenReturn(Flux.just(route));

        StepVerifier.create(repository.getRouteDefinitions())
                .assertNext(def -> {
                    FilterDefinition rl = def.getFilters().getFirst();
                    assertThat(rl.getArgs())
                            .containsEntry("redis-rate-limiter.requestedTokens", "1");
                })
                .verifyComplete();
    }

    @Test
    void getRouteDefinitions_multipleRoutes_eachGetsOwnRateLimit() {
        GatewayRoute fast = GatewayRoute.builder()
                .id("catalog-service")
                .uri("http://localhost:8083")
                .predicates(List.of("Path=/api/v1/products/**"))
                .filters(List.of())
                .enabled(true)
                .rateLimitReplenishRate(50)
                .rateLimitBurstCapacity(100)
                .rateLimitRequestedTokens(1)
                .build();

        GatewayRoute slow = GatewayRoute.builder()
                .id("payment-service")
                .uri("http://localhost:8091")
                .predicates(List.of("Path=/api/v1/payments/**"))
                .filters(List.of())
                .enabled(true)
                .rateLimitReplenishRate(5)
                .rateLimitBurstCapacity(10)
                .rateLimitRequestedTokens(1)
                .build();

        when(routeRepository.findAllEnabled()).thenReturn(Flux.just(fast, slow));

        StepVerifier.create(repository.getRouteDefinitions().collectList())
                .assertNext(defs -> {
                    assertThat(defs).hasSize(2);

                    RouteDefinition catalogDef = defs.stream()
                            .filter(d -> d.getId().equals("catalog-service"))
                            .findFirst().orElseThrow();
                    assertThat(catalogDef.getFilters().getFirst().getArgs())
                            .containsEntry("redis-rate-limiter.replenishRate", "50");

                    RouteDefinition paymentDef = defs.stream()
                            .filter(d -> d.getId().equals("payment-service"))
                            .findFirst().orElseThrow();
                    assertThat(paymentDef.getFilters().getFirst().getArgs())
                            .containsEntry("redis-rate-limiter.replenishRate", "5");
                })
                .verifyComplete();
    }

    @Test
    void getRouteDefinitions_onlyReplenishRateSet_noRateLimiterFilter() {
        GatewayRoute route = GatewayRoute.builder()
                .id("partial-config")
                .uri("http://localhost:8083")
                .predicates(List.of("Path=/api/v1/test/**"))
                .filters(List.of())
                .enabled(true)
                .rateLimitReplenishRate(10)
                // burstCapacity is null → incomplete config → no rate limit
                .build();

        when(routeRepository.findAllEnabled()).thenReturn(Flux.just(route));

        StepVerifier.create(repository.getRouteDefinitions())
                .assertNext(def -> assertThat(def.getFilters()).isEmpty())
                .verifyComplete();
    }
}
