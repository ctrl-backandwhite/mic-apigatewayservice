package com.backandwhite.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.api.dto.in.RouteDefinitionDtoIn;
import com.backandwhite.api.dto.out.RouteDefinitionDtoOut;
import com.backandwhite.api.mapper.RouteDefinitionDtoMapper;
import com.backandwhite.application.usecase.RouteManagementUseCase;
import com.backandwhite.domain.model.GatewayRoute;
import com.backandwhite.provider.route.RouteDefinitionProvider;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class RouteManagementControllerTest {

    @Mock
    private RouteManagementUseCase routeManagementUseCase;

    @Mock
    private RouteDefinitionDtoMapper mapper;

    @InjectMocks
    private RouteManagementController controller;

    private static RouteDefinitionDtoOut sampleDtoOut(String id) {
        return RouteDefinitionDtoOut.builder().id(id).uri("http://localhost:8083")
                .predicates(List.of("Path=/api/v1/products/**")).filters(List.of()).order(0).enabled(true).build();
    }

    // ------------------------------------------------------------------
    // findAll
    // ------------------------------------------------------------------

    @Test
    void findAll_shouldReturnMappedDtoList() {
        GatewayRoute route = RouteDefinitionProvider.getCatalogRoute();
        RouteDefinitionDtoOut dtoOut = sampleDtoOut("catalog-service");

        when(routeManagementUseCase.findAll()).thenReturn(Flux.just(route));
        when(mapper.toDtoOut(route)).thenReturn(dtoOut);

        StepVerifier.create(controller.findAll())
                .assertNext(dto -> assertThat(dto.getId()).isEqualTo("catalog-service")).verifyComplete();
    }

    @Test
    void findAll_whenEmpty_shouldReturnEmptyFlux() {
        when(routeManagementUseCase.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(controller.findAll()).verifyComplete();
    }

    // ------------------------------------------------------------------
    // findById
    // ------------------------------------------------------------------

    @Test
    void findById_whenFound_shouldReturn200() {
        GatewayRoute route = RouteDefinitionProvider.getCatalogRoute();
        RouteDefinitionDtoOut dtoOut = sampleDtoOut("catalog-service");

        when(routeManagementUseCase.findById("catalog-service")).thenReturn(Mono.just(route));
        when(mapper.toDtoOut(route)).thenReturn(dtoOut);

        StepVerifier.create(controller.findById("catalog-service")).assertNext(response -> {
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getId()).isEqualTo("catalog-service");
        }).verifyComplete();
    }

    // ------------------------------------------------------------------
    // create
    // ------------------------------------------------------------------

    @Test
    void create_shouldReturn201WithCreatedRoute() {
        RouteDefinitionDtoIn dtoIn = RouteDefinitionProvider.getRouteDefinitionDtoInOne();
        GatewayRoute domain = RouteDefinitionProvider.getCatalogRoute();
        RouteDefinitionDtoOut dtoOut = sampleDtoOut("catalog-service");

        when(mapper.toDomain(dtoIn)).thenReturn(domain);
        when(routeManagementUseCase.create(domain)).thenReturn(Mono.just(domain));
        when(mapper.toDtoOut(domain)).thenReturn(dtoOut);

        StepVerifier.create(controller.create(dtoIn)).assertNext(response -> {
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getId()).isEqualTo("catalog-service");
        }).verifyComplete();

        verify(routeManagementUseCase).create(domain);
    }

    // ------------------------------------------------------------------
    // update
    // ------------------------------------------------------------------

    @Test
    void update_shouldReturn200WithUpdatedRoute() {
        RouteDefinitionDtoIn dtoIn = RouteDefinitionProvider.getRouteDefinitionDtoInOne();
        GatewayRoute domain = RouteDefinitionProvider.getCatalogRoute();
        RouteDefinitionDtoOut dtoOut = sampleDtoOut("catalog-service");

        when(mapper.toDomain(dtoIn)).thenReturn(domain);
        when(routeManagementUseCase.update(domain, "catalog-service")).thenReturn(Mono.just(domain));
        when(mapper.toDtoOut(domain)).thenReturn(dtoOut);

        StepVerifier.create(controller.update("catalog-service", dtoIn)).assertNext(response -> {
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
        }).verifyComplete();

        verify(routeManagementUseCase).update(domain, "catalog-service");
    }

    // ------------------------------------------------------------------
    // delete
    // ------------------------------------------------------------------

    @Test
    void delete_shouldReturn204() {
        when(routeManagementUseCase.delete("catalog-service")).thenReturn(Mono.empty());

        StepVerifier.create(controller.delete("catalog-service"))
                .assertNext(response -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT))
                .verifyComplete();

        verify(routeManagementUseCase).delete("catalog-service");
    }

    // ------------------------------------------------------------------
    // bulkDelete
    // ------------------------------------------------------------------

    @Test
    void bulkDelete_shouldReturnDeletedCount() {
        when(routeManagementUseCase.bulkDelete(anyList())).thenReturn(Mono.just(3L));

        StepVerifier.create(controller.bulkDelete(Map.of("ids", List.of("r1", "r2", "r3")))).assertNext(response -> {
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).containsEntry("deleted", 3L);
        }).verifyComplete();
    }

    @Test
    void bulkDelete_withMissingIdsKey_shouldPassEmptyList() {
        when(routeManagementUseCase.bulkDelete(List.of())).thenReturn(Mono.just(0L));

        StepVerifier.create(controller.bulkDelete(Map.of())).assertNext(response -> {
            assertThat(response.getBody()).containsEntry("deleted", 0L);
        }).verifyComplete();
    }

    // ------------------------------------------------------------------
    // bulkImport
    // ------------------------------------------------------------------

    @Test
    void bulkImport_shouldReturnImportResult() {
        RouteDefinitionDtoIn dtoIn = RouteDefinitionProvider.getRouteDefinitionDtoInOne();
        GatewayRoute domain = RouteDefinitionProvider.getCatalogRoute();
        Map<String, Object> result = Map.of("created", 1, "skipped", 0, "skippedIds", List.of(), "errors", List.of());

        when(mapper.toDomain(dtoIn)).thenReturn(domain);
        when(routeManagementUseCase.bulkImport(anyList())).thenReturn(Mono.just(result));

        StepVerifier.create(controller.bulkImport(List.of(dtoIn))).assertNext(response -> {
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).containsEntry("created", 1);
        }).verifyComplete();
    }

    // ------------------------------------------------------------------
    // toggle
    // ------------------------------------------------------------------

    @Test
    void toggle_shouldReturn200WithToggledRoute() {
        GatewayRoute toggled = RouteDefinitionProvider.getCatalogRoute().withEnabled(false);
        RouteDefinitionDtoOut dtoOut = sampleDtoOut("catalog-service").withEnabled(false);

        when(routeManagementUseCase.toggleEnabled("catalog-service")).thenReturn(Mono.just(toggled));
        when(mapper.toDtoOut(toggled)).thenReturn(dtoOut);

        StepVerifier.create(controller.toggle("catalog-service")).assertNext(response -> {
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isEnabled()).isFalse();
        }).verifyComplete();
    }

    // ------------------------------------------------------------------
    // refresh
    // ------------------------------------------------------------------

    @Test
    void refresh_shouldReturn200() {
        when(routeManagementUseCase.refresh()).thenReturn(Mono.empty());

        StepVerifier.create(controller.refresh())
                .assertNext(response -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK)).verifyComplete();

        verify(routeManagementUseCase).refresh();
    }
}
