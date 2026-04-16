package com.backandwhite.application.usecase.impl;

import com.backandwhite.common.exception.EntityNotFoundException;
import com.backandwhite.domain.model.GatewayRoute;
import com.backandwhite.infrastructure.facade.GatewayRouteFacade;
import com.backandwhite.provider.route.RouteDefinitionProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteManagementUseCaseImplTest {

    @Mock
    private GatewayRouteFacade routeFacade;

    @InjectMocks
    private RouteManagementUseCaseImpl useCase;

    @Test
    void findAll_shouldReturnAllRoutes() {
        GatewayRoute catalogRoute = RouteDefinitionProvider.getCatalogRoute();
        GatewayRoute orderRoute = RouteDefinitionProvider.getOrderRoute();
        when(routeFacade.findAll()).thenReturn(Flux.just(catalogRoute, orderRoute));

        StepVerifier.create(useCase.findAll())
                .assertNext(r -> assertThat(r.getId()).isEqualTo(RouteDefinitionProvider.ROUTE_ID_ONE))
                .assertNext(r -> assertThat(r.getId()).isEqualTo(RouteDefinitionProvider.ROUTE_ID_TWO))
                .verifyComplete();

        verify(routeFacade).findAll();
    }

    @Test
    void findAll_whenEmpty_shouldReturnEmptyFlux() {
        when(routeFacade.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(useCase.findAll())
                .verifyComplete();
    }

    @Test
    void findById_whenFound_shouldReturnRoute() {
        GatewayRoute route = RouteDefinitionProvider.getCatalogRoute();
        when(routeFacade.findById(RouteDefinitionProvider.ROUTE_ID_ONE)).thenReturn(Mono.just(route));

        StepVerifier.create(useCase.findById(RouteDefinitionProvider.ROUTE_ID_ONE))
                .assertNext(r -> {
                    assertThat(r.getId()).isEqualTo(RouteDefinitionProvider.ROUTE_ID_ONE);
                    assertThat(r.getUri()).isEqualTo(route.getUri());
                })
                .verifyComplete();

        verify(routeFacade).findById(RouteDefinitionProvider.ROUTE_ID_ONE);
    }

    @Test
    void findById_whenNotFound_shouldReturnEntityNotFoundException() {
        when(routeFacade.findById(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.findById("non-existent-route"))
                .expectError(EntityNotFoundException.class)
                .verify();

        verify(routeFacade).findById("non-existent-route");
    }

    @Test
    void create_shouldSaveRouteViaFacade() {
        GatewayRoute route = RouteDefinitionProvider.getCatalogRoute();
        when(routeFacade.save(any(GatewayRoute.class))).thenReturn(Mono.just(route));

        StepVerifier.create(useCase.create(route))
                .assertNext(saved -> assertThat(saved.getId()).isEqualTo(RouteDefinitionProvider.ROUTE_ID_ONE))
                .verifyComplete();

        verify(routeFacade).save(any(GatewayRoute.class));
    }

    @Test
    void update_whenRouteExists_shouldUpdateViaFacade() {
        GatewayRoute existing = RouteDefinitionProvider.getCatalogRoute();
        GatewayRoute updated = RouteDefinitionProvider.getOrderRoute();
        when(routeFacade.findById(anyString())).thenReturn(Mono.just(existing));
        when(routeFacade.update(any(GatewayRoute.class), anyString())).thenReturn(Mono.just(updated));

        StepVerifier.create(useCase.update(updated, RouteDefinitionProvider.ROUTE_ID_ONE))
                .assertNext(r -> assertThat(r.getId()).isEqualTo(RouteDefinitionProvider.ROUTE_ID_TWO))
                .verifyComplete();

        verify(routeFacade).findById(anyString());
        verify(routeFacade).update(any(GatewayRoute.class), anyString());
    }

    @Test
    void update_whenRouteNotFound_shouldReturnEntityNotFoundException() {
        when(routeFacade.findById(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.update(RouteDefinitionProvider.getCatalogRoute(), "non-existent"))
                .expectError(EntityNotFoundException.class)
                .verify();

        verify(routeFacade).findById(anyString());
        verify(routeFacade, never()).update(any(), anyString());
    }

    @Test
    void delete_whenRouteExists_shouldDeleteViaFacade() {
        GatewayRoute route = RouteDefinitionProvider.getCatalogRoute();
        when(routeFacade.findById(anyString())).thenReturn(Mono.just(route));
        when(routeFacade.delete(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.delete(RouteDefinitionProvider.ROUTE_ID_ONE))
                .verifyComplete();

        verify(routeFacade).findById(anyString());
        verify(routeFacade).delete(anyString());
    }

    @Test
    void delete_whenRouteNotFound_shouldReturnEntityNotFoundException() {
        when(routeFacade.findById(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.delete("non-existent"))
                .expectError(EntityNotFoundException.class)
                .verify();

        verify(routeFacade).findById(anyString());
        verify(routeFacade, never()).delete(anyString());
    }

    @Test
    void toggleEnabled_whenRouteExists_shouldToggleViaFacade() {
        GatewayRoute route = RouteDefinitionProvider.getCatalogRoute();
        GatewayRoute toggled = RouteDefinitionProvider.getCatalogRoute().withEnabled(false);
        when(routeFacade.findById(anyString())).thenReturn(Mono.just(route));
        when(routeFacade.toggleEnabled(anyString())).thenReturn(Mono.just(toggled));

        StepVerifier.create(useCase.toggleEnabled(RouteDefinitionProvider.ROUTE_ID_ONE))
                .assertNext(r -> assertThat(r.isEnabled()).isFalse())
                .verifyComplete();

        verify(routeFacade).findById(anyString());
        verify(routeFacade).toggleEnabled(anyString());
    }

    @Test
    void toggleEnabled_whenRouteNotFound_shouldReturnEntityNotFoundException() {
        when(routeFacade.findById(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.toggleEnabled("non-existent"))
                .expectError(EntityNotFoundException.class)
                .verify();

        verify(routeFacade).findById(anyString());
        verify(routeFacade, never()).toggleEnabled(anyString());
    }

    @Test
    void refresh_shouldPublishRefreshEventViaFacade() {
        doNothing().when(routeFacade).publishRefreshEvent();

        StepVerifier.create(useCase.refresh())
                .verifyComplete();

        verify(routeFacade).publishRefreshEvent();
    }
}
