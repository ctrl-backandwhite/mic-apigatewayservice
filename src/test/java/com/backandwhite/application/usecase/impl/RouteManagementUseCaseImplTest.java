package com.backandwhite.application.usecase.impl;

import com.backandwhite.common.exception.EntityNotFoundException;
import com.backandwhite.domain.model.GatewayRoute;
import com.backandwhite.domain.repository.GatewayRouteRepository;
import com.backandwhite.provider.route.RouteDefinitionProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteManagementUseCaseImplTest {

    @Mock
    private GatewayRouteRepository routeRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RouteManagementUseCaseImpl useCase;

    @Test
    void findAll_shouldReturnAllRoutes() {
        // Arrange
        GatewayRoute catalogRoute = RouteDefinitionProvider.getCatalogRoute();
        GatewayRoute orderRoute = RouteDefinitionProvider.getOrderRoute();
        when(routeRepository.findAll()).thenReturn(Flux.just(catalogRoute, orderRoute));

        // Act & Assert
        StepVerifier.create(useCase.findAll())
                .assertNext(r -> assertThat(r.getId()).isEqualTo(RouteDefinitionProvider.ROUTE_ID_ONE))
                .assertNext(r -> assertThat(r.getId()).isEqualTo(RouteDefinitionProvider.ROUTE_ID_TWO))
                .verifyComplete();

        verify(routeRepository).findAll();
    }

    @Test
    void findAll_whenEmpty_shouldReturnEmptyFlux() {
        when(routeRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(useCase.findAll())
                .verifyComplete();
    }

    @Test
    void findById_whenFound_shouldReturnRoute() {
        // Arrange
        GatewayRoute route = RouteDefinitionProvider.getCatalogRoute();
        when(routeRepository.findById(RouteDefinitionProvider.ROUTE_ID_ONE)).thenReturn(Mono.just(route));

        // Act & Assert
        StepVerifier.create(useCase.findById(RouteDefinitionProvider.ROUTE_ID_ONE))
                .assertNext(r -> {
                    assertThat(r.getId()).isEqualTo(RouteDefinitionProvider.ROUTE_ID_ONE);
                    assertThat(r.getUri()).isEqualTo(route.getUri());
                })
                .verifyComplete();

        verify(routeRepository).findById(RouteDefinitionProvider.ROUTE_ID_ONE);
    }

    @Test
    void findById_whenNotFound_shouldReturnEntityNotFoundException() {
        when(routeRepository.findById(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.findById("non-existent-route"))
                .expectError(EntityNotFoundException.class)
                .verify();

        verify(routeRepository).findById("non-existent-route");
    }

    @Test
    void create_shouldSaveRouteAndPublishRefreshEvent() {
        // Arrange
        GatewayRoute route = RouteDefinitionProvider.getCatalogRoute();
        when(routeRepository.save(any(GatewayRoute.class))).thenReturn(Mono.just(route));

        // Act & Assert
        StepVerifier.create(useCase.create(route))
                .assertNext(saved -> assertThat(saved.getId()).isEqualTo(RouteDefinitionProvider.ROUTE_ID_ONE))
                .verifyComplete();

        verify(routeRepository).save(any(GatewayRoute.class));
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void update_whenRouteExists_shouldUpdateAndPublishRefreshEvent() {
        // Arrange
        GatewayRoute existing = RouteDefinitionProvider.getCatalogRoute();
        GatewayRoute updated = RouteDefinitionProvider.getOrderRoute();
        when(routeRepository.findById(anyString())).thenReturn(Mono.just(existing));
        when(routeRepository.update(any(GatewayRoute.class), anyString())).thenReturn(Mono.just(updated));

        // Act & Assert
        StepVerifier.create(useCase.update(updated, RouteDefinitionProvider.ROUTE_ID_ONE))
                .assertNext(r -> assertThat(r.getId()).isEqualTo(RouteDefinitionProvider.ROUTE_ID_TWO))
                .verifyComplete();

        verify(routeRepository).findById(anyString());
        verify(routeRepository).update(any(GatewayRoute.class), anyString());
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void update_whenRouteNotFound_shouldReturnEntityNotFoundException() {
        when(routeRepository.findById(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.update(RouteDefinitionProvider.getCatalogRoute(), "non-existent"))
                .expectError(EntityNotFoundException.class)
                .verify();

        verify(routeRepository).findById(anyString());
        verify(routeRepository, never()).update(any(), anyString());
    }

    @Test
    void delete_whenRouteExists_shouldDeleteAndPublishRefreshEvent() {
        // Arrange
        GatewayRoute route = RouteDefinitionProvider.getCatalogRoute();
        when(routeRepository.findById(anyString())).thenReturn(Mono.just(route));
        when(routeRepository.delete(anyString())).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(useCase.delete(RouteDefinitionProvider.ROUTE_ID_ONE))
                .verifyComplete();

        verify(routeRepository).findById(anyString());
        verify(routeRepository).delete(anyString());
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void delete_whenRouteNotFound_shouldReturnEntityNotFoundException() {
        when(routeRepository.findById(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.delete("non-existent"))
                .expectError(EntityNotFoundException.class)
                .verify();

        verify(routeRepository).findById(anyString());
        verify(routeRepository, never()).delete(anyString());
    }

    @Test
    void toggleEnabled_whenRouteExists_shouldToggleAndPublishRefreshEvent() {
        // Arrange
        GatewayRoute route = RouteDefinitionProvider.getCatalogRoute();
        GatewayRoute toggled = RouteDefinitionProvider.getCatalogRoute().withEnabled(false);
        when(routeRepository.findById(anyString())).thenReturn(Mono.just(route));
        when(routeRepository.toggleEnabled(anyString())).thenReturn(Mono.just(toggled));

        // Act & Assert
        StepVerifier.create(useCase.toggleEnabled(RouteDefinitionProvider.ROUTE_ID_ONE))
                .assertNext(r -> assertThat(r.isEnabled()).isFalse())
                .verifyComplete();

        verify(routeRepository).findById(anyString());
        verify(routeRepository).toggleEnabled(anyString());
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void toggleEnabled_whenRouteNotFound_shouldReturnEntityNotFoundException() {
        when(routeRepository.findById(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.toggleEnabled("non-existent"))
                .expectError(EntityNotFoundException.class)
                .verify();

        verify(routeRepository).findById(anyString());
        verify(routeRepository, never()).toggleEnabled(anyString());
    }

    @Test
    void refresh_shouldPublishRefreshEvent() {
        StepVerifier.create(useCase.refresh())
                .verifyComplete();

        verify(eventPublisher).publishEvent(any());
    }
}
