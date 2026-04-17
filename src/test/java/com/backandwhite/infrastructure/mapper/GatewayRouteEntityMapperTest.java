package com.backandwhite.infrastructure.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.backandwhite.domain.model.GatewayRoute;
import com.backandwhite.infrastructure.entity.GatewayRouteEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class GatewayRouteEntityMapperTest {

    private GatewayRouteEntityMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new GatewayRouteEntityMapper(new ObjectMapper());
    }

    @Test
    void toDomain_shouldMapEntityToDomain() {
        GatewayRouteEntity entity = GatewayRouteEntity.builder().id("test-route").uri("http://localhost:8080")
                .predicates("[\"Path=/api/v1/test/**\"]").filters("[\"StripPrefix=1\"]").order(0).enabled(true)
                .rateLimitReplenishRate(10).rateLimitBurstCapacity(20).rateLimitRequestedTokens(1)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        GatewayRoute domain = mapper.toDomain(entity);

        assertThat(domain.getId()).isEqualTo("test-route");
        assertThat(domain.getUri()).isEqualTo("http://localhost:8080");
        assertThat(domain.getPredicates()).containsExactly("Path=/api/v1/test/**");
        assertThat(domain.getFilters()).containsExactly("StripPrefix=1");
        assertThat(domain.getOrder()).isZero();
        assertThat(domain.isEnabled()).isTrue();
        assertThat(domain.getRateLimitReplenishRate()).isEqualTo(10);
        assertThat(domain.getRateLimitBurstCapacity()).isEqualTo(20);
        assertThat(domain.getRateLimitRequestedTokens()).isEqualTo(1);
    }

    @Test
    void toDomain_withNullPredicatesAndFilters_shouldReturnEmptyLists() {
        GatewayRouteEntity entity = GatewayRouteEntity.builder().id("test-route").uri("http://localhost:8080")
                .predicates(null).filters(null).order(0).enabled(true).build();

        GatewayRoute domain = mapper.toDomain(entity);

        assertThat(domain.getPredicates()).isEmpty();
        assertThat(domain.getFilters()).isEmpty();
    }

    @Test
    void toDomain_withInvalidJson_shouldReturnEmptyLists() {
        GatewayRouteEntity entity = GatewayRouteEntity.builder().id("test-route").uri("http://localhost:8080")
                .predicates("not-valid-json").filters("{invalid}").order(0).enabled(true).build();

        GatewayRoute domain = mapper.toDomain(entity);

        assertThat(domain.getPredicates()).isEmpty();
        assertThat(domain.getFilters()).isEmpty();
    }

    @Test
    void toEntity_shouldMapDomainToEntity() {
        GatewayRoute domain = GatewayRoute.builder().id("test-route").uri("http://localhost:8080")
                .predicates(List.of("Path=/api/v1/test/**")).filters(List.of("StripPrefix=1")).order(0).enabled(true)
                .rateLimitReplenishRate(10).rateLimitBurstCapacity(20).rateLimitRequestedTokens(1)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        GatewayRouteEntity entity = mapper.toEntity(domain);

        assertThat(entity.getId()).isEqualTo("test-route");
        assertThat(entity.getUri()).isEqualTo("http://localhost:8080");
        assertThat(entity.getPredicates()).isEqualTo("[\"Path=/api/v1/test/**\"]");
        assertThat(entity.getFilters()).isEqualTo("[\"StripPrefix=1\"]");
        assertThat(entity.getOrder()).isZero();
        assertThat(entity.isEnabled()).isTrue();
    }

    @Test
    void toEntity_withNullFilters_shouldReturnEmptyJsonArray() {
        GatewayRoute domain = GatewayRoute.builder().id("test-route").uri("http://localhost:8080")
                .predicates(List.of("Path=/api/**")).filters(null).order(0).enabled(true).build();

        GatewayRouteEntity entity = mapper.toEntity(domain);

        assertThat(entity.getFilters()).isEqualTo("[]");
    }

    @Test
    void roundTrip_shouldPreserveData() {
        GatewayRoute original = GatewayRoute.builder().id("round-trip").uri("http://localhost:9090")
                .predicates(List.of("Path=/api/v1/round/**", "Method=GET,POST"))
                .filters(List.of("StripPrefix=1", "AddRequestHeader=X-Test, value")).order(5).enabled(false)
                .rateLimitReplenishRate(5).rateLimitBurstCapacity(10).rateLimitRequestedTokens(2).build();

        GatewayRouteEntity entity = mapper.toEntity(original);
        GatewayRoute restored = mapper.toDomain(entity);

        assertThat(restored.getId()).isEqualTo(original.getId());
        assertThat(restored.getUri()).isEqualTo(original.getUri());
        assertThat(restored.getPredicates()).isEqualTo(original.getPredicates());
        assertThat(restored.getFilters()).isEqualTo(original.getFilters());
        assertThat(restored.getOrder()).isEqualTo(original.getOrder());
        assertThat(restored.isEnabled()).isEqualTo(original.isEnabled());
    }
}
