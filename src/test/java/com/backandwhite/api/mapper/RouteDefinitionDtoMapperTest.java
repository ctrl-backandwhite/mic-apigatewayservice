package com.backandwhite.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.backandwhite.api.dto.in.RouteDefinitionDtoIn;
import com.backandwhite.api.dto.out.RouteDefinitionDtoOut;
import com.backandwhite.domain.model.GatewayRoute;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class RouteDefinitionDtoMapperTest {

    private RouteDefinitionDtoMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(RouteDefinitionDtoMapper.class);
    }

    // ------------------------------------------------------------------
    // toDomain
    // ------------------------------------------------------------------

    @Test
    void toDomain_shouldMapAllFieldsFromDtoIn() {
        RouteDefinitionDtoIn dto = RouteDefinitionDtoIn.builder().id("catalog-service").uri("http://localhost:8083")
                .predicates(List.of("Path=/api/v1/products/**")).filters(List.of("StripPrefix=1")).order(2)
                .rateLimitReplenishRate(10).rateLimitBurstCapacity(20).rateLimitRequestedTokens(3).build();

        GatewayRoute domain = mapper.toDomain(dto);

        assertThat(domain.getId()).isEqualTo("catalog-service");
        assertThat(domain.getUri()).isEqualTo("http://localhost:8083");
        assertThat(domain.getPredicates()).containsExactly("Path=/api/v1/products/**");
        assertThat(domain.getFilters()).containsExactly("StripPrefix=1");
        assertThat(domain.getOrder()).isEqualTo(2);
        assertThat(domain.isEnabled()).isTrue(); // @Mapping constant "true"
        assertThat(domain.getRateLimitReplenishRate()).isEqualTo(10);
        assertThat(domain.getRateLimitBurstCapacity()).isEqualTo(20);
        assertThat(domain.getRateLimitRequestedTokens()).isEqualTo(3);
        assertThat(domain.getCreatedAt()).isNull(); // ignored
        assertThat(domain.getUpdatedAt()).isNull(); // ignored
    }

    @Test
    void toDomain_withNullOptionalFields_shouldMapCorrectly() {
        RouteDefinitionDtoIn dto = RouteDefinitionDtoIn.builder().id("svc").uri("http://localhost:8080")
                .predicates(List.of("Path=/api/**")).filters(null).order(0).build();

        GatewayRoute domain = mapper.toDomain(dto);

        assertThat(domain.getFilters()).isNull();
        assertThat(domain.getRateLimitReplenishRate()).isNull();
        assertThat(domain.getRateLimitBurstCapacity()).isNull();
        assertThat(domain.getRateLimitRequestedTokens()).isNull();
    }

    // ------------------------------------------------------------------
    // toDtoOut
    // ------------------------------------------------------------------

    @Test
    void toDtoOut_shouldMapAllFieldsFromDomain() {
        LocalDateTime now = LocalDateTime.now();
        GatewayRoute domain = GatewayRoute.builder().id("order-service").uri("http://localhost:9090")
                .predicates(List.of("Path=/api/v1/orders/**"))
                .filters(List.of("StripPrefix=1", "AddRequestHeader=X-Test, true")).order(5).enabled(false)
                .rateLimitReplenishRate(50).rateLimitBurstCapacity(100).rateLimitRequestedTokens(2).createdAt(now)
                .updatedAt(now).build();

        RouteDefinitionDtoOut dto = mapper.toDtoOut(domain);

        assertThat(dto.getId()).isEqualTo("order-service");
        assertThat(dto.getUri()).isEqualTo("http://localhost:9090");
        assertThat(dto.getPredicates()).containsExactly("Path=/api/v1/orders/**");
        assertThat(dto.getFilters()).containsExactly("StripPrefix=1", "AddRequestHeader=X-Test, true");
        assertThat(dto.getOrder()).isEqualTo(5);
        assertThat(dto.isEnabled()).isFalse();
        assertThat(dto.getRateLimitReplenishRate()).isEqualTo(50);
        assertThat(dto.getRateLimitBurstCapacity()).isEqualTo(100);
        assertThat(dto.getRateLimitRequestedTokens()).isEqualTo(2);
        assertThat(dto.getCreatedAt()).isEqualTo(now);
        assertThat(dto.getUpdatedAt()).isEqualTo(now);
    }

    // ------------------------------------------------------------------
    // toDtoOutList
    // ------------------------------------------------------------------

    @Test
    void toDtoOutList_shouldMapAllElements() {
        GatewayRoute r1 = GatewayRoute.builder().id("svc-1").uri("http://localhost:8081")
                .predicates(List.of("Path=/api/v1/svc1/**")).filters(List.of()).order(0).enabled(true).build();
        GatewayRoute r2 = GatewayRoute.builder().id("svc-2").uri("http://localhost:8082")
                .predicates(List.of("Path=/api/v1/svc2/**")).filters(List.of()).order(1).enabled(false).build();

        List<RouteDefinitionDtoOut> result = mapper.toDtoOutList(List.of(r1, r2));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo("svc-1");
        assertThat(result.get(1).getId()).isEqualTo("svc-2");
    }

    @Test
    void toDtoOutList_whenEmpty_shouldReturnEmptyList() {
        assertThat(mapper.toDtoOutList(List.of())).isEmpty();
    }

    // ------------------------------------------------------------------
    // Null safety
    // ------------------------------------------------------------------

    @Test
    void toDomain_whenNull_shouldReturnNull() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void toDtoOut_whenNull_shouldReturnNull() {
        assertThat(mapper.toDtoOut(null)).isNull();
    }

    @Test
    void toDtoOutList_whenNull_shouldReturnNull() {
        assertThat(mapper.toDtoOutList(null)).isNull();
    }
}
