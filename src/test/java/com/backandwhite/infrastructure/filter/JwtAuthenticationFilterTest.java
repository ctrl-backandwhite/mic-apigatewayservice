package com.backandwhite.infrastructure.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.backandwhite.common.security.jwt.JwtProperties;
import com.backandwhite.provider.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        lenient().when(jwtProperties.secret()).thenReturn(JwtTokenProvider.TEST_SECRET);
        lenient().when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void filter_withValidCustomerToken_shouldProceedAndEnrichHeaders() {
        String token = JwtTokenProvider.customerToken("customer@test.com");
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/orders/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(any());
    }

    @Test
    void filter_withValidAdminToken_shouldProceed() {
        String token = JwtTokenProvider.adminToken("admin@test.com");
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .get("/api/v1/analytics/dashboard").header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(any());
    }

    @Test
    void filter_withMissingAuthorizationHeader_shouldReturnUnauthorized() {
        MockServerWebExchange exchange = MockServerWebExchange
                .from(MockServerHttpRequest.get("/api/v1/orders/1").build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_withInvalidBearerPrefix_shouldReturnUnauthorized() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/orders/1")
                .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz").build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_withExpiredToken_shouldReturnUnauthorized() {
        String expiredToken = JwtTokenProvider.expiredToken("user@test.com");
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/orders/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken).build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_withMalformedToken_shouldReturnUnauthorized() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/orders/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer not.a.real.jwt").build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_onPublicPostPath_shouldProceedWithoutToken() {
        MockServerWebExchange exchange = MockServerWebExchange
                .from(MockServerHttpRequest.post("/api/v1/auth/login").build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(any());
        verify(jwtProperties, never()).secret();
    }

    @Test
    void filter_onPublicGetCatalogPath_shouldProceedWithoutToken() {
        MockServerWebExchange exchange = MockServerWebExchange
                .from(MockServerHttpRequest.get("/api/v1/products/123").build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(any());
        verify(jwtProperties, never()).secret();
    }

    @Test
    void filter_onPublicGetProductsPostWithoutToken_shouldReturnUnauthorized() {
        // POST /api/v1/products requiere autenticación aunque GET sea público
        MockServerWebExchange exchange = MockServerWebExchange
                .from(MockServerHttpRequest.post("/api/v1/products").build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void filter_onActuatorPath_shouldProceedWithoutToken() {
        MockServerWebExchange exchange = MockServerWebExchange
                .from(MockServerHttpRequest.get("/actuator/health").build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(any());
    }

    @Test
    void filter_orderShouldBeMinusHundred() {
        assertThat(filter.getOrder()).isEqualTo(-100);
    }
}
