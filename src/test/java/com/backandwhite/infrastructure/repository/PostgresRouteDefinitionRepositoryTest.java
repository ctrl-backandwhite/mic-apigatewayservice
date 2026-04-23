package com.backandwhite.infrastructure.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.backandwhite.domain.model.GatewayRoute;
import com.backandwhite.domain.repository.GatewayRouteRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class PostgresRouteDefinitionRepositoryTest {

    @Mock
    private GatewayRouteRepository routeRepository;

    @InjectMocks
    private PostgresRouteDefinitionRepository repository;

    // ------------------------------------------------------------------
    // getRouteDefinitions
    // ------------------------------------------------------------------

    @Test
    void getRouteDefinitions_shouldConvertEnabledRoutesToRouteDefinitions() {
        GatewayRoute route = GatewayRoute.builder().id("catalog-service").uri("http://localhost:8083")
                .predicates(List.of("Path=/api/v1/products/**")).filters(List.of("StripPrefix=1")).order(1)
                .enabled(true).build();

        when(routeRepository.findAllEnabled()).thenReturn(Flux.just(route));

        StepVerifier.create(repository.getRouteDefinitions()).assertNext(rd -> {
            assertThat(rd.getId()).isEqualTo("catalog-service");
            assertThat(rd.getUri()).hasToString("http://localhost:8083");
            assertThat(rd.getOrder()).isEqualTo(1);
            assertThat(rd.getPredicates()).hasSize(1);
            assertThat(rd.getPredicates().getFirst().getName()).isEqualTo("Path");
            assertThat(rd.getFilters()).hasSize(1);
            assertThat(rd.getFilters().getFirst().getName()).isEqualTo("StripPrefix");
        }).verifyComplete();
    }

    @Test
    void getRouteDefinitions_whenEmpty_shouldReturnEmptyFlux() {
        when(routeRepository.findAllEnabled()).thenReturn(Flux.empty());

        StepVerifier.create(repository.getRouteDefinitions()).verifyComplete();
    }

    @Test
    void getRouteDefinitions_whenError_shouldReturnEmptyFlux() {
        when(routeRepository.findAllEnabled()).thenReturn(Flux.error(new RuntimeException("DB connection failed")));

        StepVerifier.create(repository.getRouteDefinitions()).verifyComplete();
    }

    // ------------------------------------------------------------------
    // Rate limiter injection
    // ------------------------------------------------------------------

    @Test
    void getRouteDefinitions_withRateLimitConfig_shouldInjectRateLimiterFilter() {
        GatewayRoute route = GatewayRoute.builder().id("rate-limited-svc").uri("http://localhost:9090")
                .predicates(List.of("Path=/api/v1/orders/**")).filters(List.of()).order(0).enabled(true)
                .rateLimitReplenishRate(10).rateLimitBurstCapacity(20).rateLimitRequestedTokens(2).build();

        when(routeRepository.findAllEnabled()).thenReturn(Flux.just(route));

        StepVerifier.create(repository.getRouteDefinitions()).assertNext(rd -> {
            assertThat(rd.getFilters()).hasSize(1);
            assertThat(rd.getFilters().getFirst().getName()).isEqualTo("RequestRateLimiter");
            assertThat(rd.getFilters().getFirst().getArgs()).containsEntry("redis-rate-limiter.replenishRate", "10")
                    .containsEntry("redis-rate-limiter.burstCapacity", "20")
                    .containsEntry("redis-rate-limiter.requestedTokens", "2")
                    .containsEntry("key-resolver", "#{@ipKeyResolver}").containsEntry("deny-empty-key", "true");
        }).verifyComplete();
    }

    @Test
    void getRouteDefinitions_withRateLimitDefaultTokens_shouldDefaultTo1() {
        GatewayRoute route = GatewayRoute.builder().id("svc").uri("http://localhost:9090")
                .predicates(List.of("Path=/api/**")).filters(List.of()).order(0).enabled(true).rateLimitReplenishRate(5)
                .rateLimitBurstCapacity(10).rateLimitRequestedTokens(null).build();

        when(routeRepository.findAllEnabled()).thenReturn(Flux.just(route));

        StepVerifier.create(repository.getRouteDefinitions())
                .assertNext(rd -> assertThat(rd.getFilters().getFirst().getArgs())
                        .containsEntry("redis-rate-limiter.requestedTokens", "1"))
                .verifyComplete();
    }

    @Test
    void getRouteDefinitions_withoutRateLimit_shouldNotAddRateLimiterFilter() {
        GatewayRoute route = GatewayRoute.builder().id("no-limit-svc").uri("http://localhost:8080")
                .predicates(List.of("Path=/api/v1/health")).filters(List.of("AddRequestHeader=X-Custom, value"))
                .order(0).enabled(true).build();

        when(routeRepository.findAllEnabled()).thenReturn(Flux.just(route));

        StepVerifier.create(repository.getRouteDefinitions()).assertNext(rd -> {
            assertThat(rd.getFilters()).hasSize(1);
            assertThat(rd.getFilters().getFirst().getName()).isEqualTo("AddRequestHeader");
        }).verifyComplete();
    }

    @Test
    void getRouteDefinitions_withExplicitFiltersAndRateLimit_shouldIncludeBoth() {
        GatewayRoute route = GatewayRoute.builder().id("combined-svc").uri("http://localhost:8080")
                .predicates(List.of("Path=/api/**")).filters(List.of("StripPrefix=1")).order(0).enabled(true)
                .rateLimitReplenishRate(100).rateLimitBurstCapacity(200).build();

        when(routeRepository.findAllEnabled()).thenReturn(Flux.just(route));

        StepVerifier.create(repository.getRouteDefinitions()).assertNext(rd -> {
            assertThat(rd.getFilters()).hasSize(2);
            assertThat(rd.getFilters().get(0).getName()).isEqualTo("StripPrefix");
            assertThat(rd.getFilters().get(1).getName()).isEqualTo("RequestRateLimiter");
        }).verifyComplete();
    }

    // ------------------------------------------------------------------
    // Null predicates / filters
    // ------------------------------------------------------------------

    @Test
    void getRouteDefinitions_withNullPredicates_shouldReturnEmptyPredicateList() {
        GatewayRoute route = GatewayRoute.builder().id("null-preds").uri("http://localhost:8080").predicates(null)
                .filters(null).order(0).enabled(true).build();

        when(routeRepository.findAllEnabled()).thenReturn(Flux.just(route));

        StepVerifier.create(repository.getRouteDefinitions()).assertNext(rd -> {
            assertThat(rd.getPredicates()).isEmpty();
            assertThat(rd.getFilters()).isEmpty();
        }).verifyComplete();
    }

    // ------------------------------------------------------------------
    // save / delete (no-op)
    // ------------------------------------------------------------------

    @Test
    void save_shouldCompleteEmpty() {
        StepVerifier.create(repository.save(null)).verifyComplete();
    }

    @Test
    void delete_shouldCompleteEmpty() {
        StepVerifier.create(repository.delete(null)).verifyComplete();
    }

    // ------------------------------------------------------------------
    // Multiple routes
    // ------------------------------------------------------------------

    @Test
    void getRouteDefinitions_multipleRoutes_shouldConvertAll() {
        GatewayRoute r1 = GatewayRoute.builder().id("svc-1").uri("http://localhost:8081")
                .predicates(List.of("Path=/api/v1/svc1/**")).filters(List.of()).order(0).enabled(true).build();
        GatewayRoute r2 = GatewayRoute.builder().id("svc-2").uri("http://localhost:8082")
                .predicates(List.of("Path=/api/v1/svc2/**")).filters(List.of()).order(1).enabled(true).build();

        when(routeRepository.findAllEnabled()).thenReturn(Flux.just(r1, r2));

        StepVerifier.create(repository.getRouteDefinitions())
                .assertNext(rd -> assertThat(rd.getId()).isEqualTo("svc-1"))
                .assertNext(rd -> assertThat(rd.getId()).isEqualTo("svc-2")).verifyComplete();
    }
}
