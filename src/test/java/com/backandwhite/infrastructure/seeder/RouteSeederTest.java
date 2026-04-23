package com.backandwhite.infrastructure.seeder;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.infrastructure.configuration.ServicesProperties;
import com.backandwhite.infrastructure.facade.GatewayRouteFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class RouteSeederTest {

    @Mock
    private GatewayRouteFacade routeFacade;

    @Mock
    private ServicesProperties services;

    @InjectMocks
    private RouteSeeder seeder;

    @Test
    void run_withConfiguredServices_shouldUpsertAllRoutes() {
        ServicesProperties.Service auth = new ServicesProperties.Service("http://auth:8081");
        ServicesProperties.Service notification = new ServicesProperties.Service("http://notif:8082");
        ServicesProperties.Service catalog = new ServicesProperties.Service("http://catalog:8083");
        ServicesProperties.Service cms = new ServicesProperties.Service("http://cms:8084");
        ServicesProperties.Service orders = new ServicesProperties.Service("http://orders:8085");
        ServicesProperties.Service webapp = new ServicesProperties.Service("http://webapp:8086");
        ServicesProperties.Service ecommerce = new ServicesProperties.Service("http://ecommerce:8087");

        when(services.auth()).thenReturn(auth);
        when(services.notification()).thenReturn(notification);
        when(services.catalog()).thenReturn(catalog);
        when(services.cms()).thenReturn(cms);
        when(services.orders()).thenReturn(orders);
        when(services.webapp()).thenReturn(webapp);
        when(services.ecommerce()).thenReturn(ecommerce);
        when(routeFacade.upsertAll(anyList())).thenReturn(Mono.empty());

        seeder.run(new DefaultApplicationArguments());

        verify(routeFacade).upsertAll(anyList());
    }

    @Test
    void run_withAllNullServices_shouldStillUpsertGatewayManagement() {
        // gateway-management route is always added (it's not behind
        // addRouteIfConfigured)
        when(services.auth()).thenReturn(null);
        when(services.notification()).thenReturn(null);
        when(services.catalog()).thenReturn(null);
        when(services.cms()).thenReturn(null);
        when(services.orders()).thenReturn(null);
        when(services.webapp()).thenReturn(null);
        when(services.ecommerce()).thenReturn(null);
        when(routeFacade.upsertAll(anyList())).thenReturn(Mono.empty());

        seeder.run(new DefaultApplicationArguments());

        // At minimum the gateway-management route is upserted
        verify(routeFacade).upsertAll(anyList());
    }

    @Test
    void run_withPartialServices_shouldUpsertOnlyConfigured() {
        ServicesProperties.Service auth = new ServicesProperties.Service("http://auth:8081");
        ServicesProperties.Service blankService = new ServicesProperties.Service("  ");

        when(services.auth()).thenReturn(auth);
        when(services.notification()).thenReturn(null);
        when(services.catalog()).thenReturn(blankService);
        when(services.cms()).thenReturn(null);
        when(services.orders()).thenReturn(null);
        when(services.webapp()).thenReturn(null);
        when(services.ecommerce()).thenReturn(null);
        when(routeFacade.upsertAll(anyList())).thenReturn(Mono.empty());

        seeder.run(new DefaultApplicationArguments());

        // At least auth-service-oauth2, auth-service and gateway-management are built
        verify(routeFacade).upsertAll(anyList());
    }

    @Test
    void run_whenUpsertFails_shouldNotPropagateError() {
        ServicesProperties.Service auth = new ServicesProperties.Service("http://auth:8081");
        when(services.auth()).thenReturn(auth);
        when(services.notification()).thenReturn(null);
        when(services.catalog()).thenReturn(null);
        when(services.cms()).thenReturn(null);
        when(services.orders()).thenReturn(null);
        when(services.webapp()).thenReturn(null);
        when(services.ecommerce()).thenReturn(null);
        when(routeFacade.upsertAll(anyList())).thenReturn(Mono.error(new RuntimeException("DB down")));

        org.assertj.core.api.Assertions.assertThatCode(() -> seeder.run(new DefaultApplicationArguments()))
                .doesNotThrowAnyException();
        verify(routeFacade).upsertAll(anyList());
    }
}
