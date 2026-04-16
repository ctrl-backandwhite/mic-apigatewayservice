package com.backandwhite.infrastructure.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.domain.model.GatewayRoute;
import com.backandwhite.domain.repository.GatewayRouteRepository;
import com.backandwhite.provider.route.RouteDefinitionProvider;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class GatewayRouteFacadeTest {

    @Mock
    private GatewayRouteRepository routeRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private GatewayRouteFacade facade;

    @Test
    void findAll_shouldDelegateToRepository() {
        GatewayRoute route = RouteDefinitionProvider.getCatalogRoute();
        when(routeRepository.findAll()).thenReturn(Flux.just(route));

        StepVerifier.create(facade.findAll())
                .assertNext(r -> assertThat(r.getId()).isEqualTo(RouteDefinitionProvider.ROUTE_ID_ONE))
                .verifyComplete();

        verify(routeRepository).findAll();
    }

    @Test
    void findById_shouldDelegateToRepository() {
        GatewayRoute route = RouteDefinitionProvider.getCatalogRoute();
        when(routeRepository.findById(RouteDefinitionProvider.ROUTE_ID_ONE)).thenReturn(Mono.just(route));

        StepVerifier.create(facade.findById(RouteDefinitionProvider.ROUTE_ID_ONE))
                .assertNext(r -> assertThat(r.getId()).isEqualTo(RouteDefinitionProvider.ROUTE_ID_ONE))
                .verifyComplete();

        verify(routeRepository).findById(RouteDefinitionProvider.ROUTE_ID_ONE);
    }

    @Test
    void save_shouldPersistAndPublishRefreshEvent() {
        GatewayRoute route = RouteDefinitionProvider.getCatalogRoute();
        when(routeRepository.save(any(GatewayRoute.class))).thenReturn(Mono.just(route));

        StepVerifier.create(facade.save(route))
                .assertNext(saved -> assertThat(saved.getId()).isEqualTo(RouteDefinitionProvider.ROUTE_ID_ONE))
                .verifyComplete();

        verify(routeRepository).save(any(GatewayRoute.class));
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void update_shouldPersistAndPublishRefreshEvent() {
        GatewayRoute route = RouteDefinitionProvider.getCatalogRoute();
        when(routeRepository.update(any(GatewayRoute.class), anyString())).thenReturn(Mono.just(route));

        StepVerifier.create(facade.update(route, RouteDefinitionProvider.ROUTE_ID_ONE))
                .assertNext(updated -> assertThat(updated.getId()).isEqualTo(RouteDefinitionProvider.ROUTE_ID_ONE))
                .verifyComplete();

        verify(routeRepository).update(any(GatewayRoute.class), anyString());
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void delete_shouldRemoveAndPublishRefreshEvent() {
        when(routeRepository.delete(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(facade.delete(RouteDefinitionProvider.ROUTE_ID_ONE)).verifyComplete();

        verify(routeRepository).delete(anyString());
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void toggleEnabled_shouldToggleAndPublishRefreshEvent() {
        GatewayRoute toggled = RouteDefinitionProvider.getCatalogRoute().withEnabled(false);
        when(routeRepository.toggleEnabled(anyString())).thenReturn(Mono.just(toggled));

        StepVerifier.create(facade.toggleEnabled(RouteDefinitionProvider.ROUTE_ID_ONE))
                .assertNext(r -> assertThat(r.isEnabled()).isFalse()).verifyComplete();

        verify(routeRepository).toggleEnabled(anyString());
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void upsert_shouldDelegateToRepository() {
        GatewayRoute route = RouteDefinitionProvider.getCatalogRoute();
        when(routeRepository.upsert(any(GatewayRoute.class))).thenReturn(Mono.empty());

        StepVerifier.create(facade.upsert(route)).verifyComplete();

        verify(routeRepository).upsert(any(GatewayRoute.class));
    }

    @Test
    void upsertAll_shouldUpsertAllRoutesAndPublishRefreshEvent() {
        GatewayRoute route1 = RouteDefinitionProvider.getCatalogRoute();
        GatewayRoute route2 = RouteDefinitionProvider.getOrderRoute();
        when(routeRepository.upsert(any(GatewayRoute.class))).thenReturn(Mono.empty());

        StepVerifier.create(facade.upsertAll(List.of(route1, route2))).verifyComplete();

        verify(routeRepository, times(2)).upsert(any(GatewayRoute.class));
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void publishRefreshEvent_shouldPublishEvent() {
        facade.publishRefreshEvent();

        verify(eventPublisher).publishEvent(any());
    }
}
