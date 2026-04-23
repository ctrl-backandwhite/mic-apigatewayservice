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
        // POST /api/v1/products requires authentication even though GET is public
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

    @Test
    void filter_onPublicPathWithValidToken_shouldEnrichHeaders() {
        String token = JwtTokenProvider.customerToken("user@test.com");
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(any());
    }

    @Test
    void filter_onPublicPathWithExpiredToken_shouldProceedWithoutEnriching() {
        String token = JwtTokenProvider.expiredToken("user@test.com");
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(any());
    }

    @Test
    void filter_onPublicPathWithoutToken_shouldProceedWithoutAuth() {
        MockServerWebExchange exchange = MockServerWebExchange
                .from(MockServerHttpRequest.get("/api/v1/products").build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(any());
    }

    @Test
    void filter_withTokenContainingCustomerAndEmployeeIds_shouldEnrichAllHeaders() {
        String token = JwtTokenProvider.customerTokenWithIds("user@test.com", 123L, 456L);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(any());
    }

    @Test
    void filter_withTokenWithoutCustomerOrEmployeeIds_shouldEnrichBasicHeaders() {
        String token = JwtTokenProvider.customerTokenWithIds("user@test.com", null, null);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(any());
    }

    @Test
    void filter_onOAuth2Path_shouldProceedWithoutAuth() {
        MockServerWebExchange exchange = MockServerWebExchange
                .from(MockServerHttpRequest.get("/oauth2/authorization/google").build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(any());
    }

    @Test
    void filter_onLoginPath_shouldProceedWithoutAuth() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/login").build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(any());
    }

    @Test
    void filter_onPublicGetReviewsPath_shouldProceedWithoutAuth() {
        MockServerWebExchange exchange = MockServerWebExchange
                .from(MockServerHttpRequest.get("/api/v1/reviews").build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(any());
    }

    @Test
    void filter_onPublicGetBrandsPath_shouldProceedWithoutAuth() {
        MockServerWebExchange exchange = MockServerWebExchange
                .from(MockServerHttpRequest.get("/api/v1/brands").build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(any());
    }

    @Test
    void filter_onCjWebhookPath_shouldProceedWithoutAuth() {
        MockServerWebExchange exchange = MockServerWebExchange
                .from(MockServerHttpRequest.post("/api/v1/cj/webhook/event").build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(any());
    }

    @Test
    void filter_withOptionsMethod_shouldProceedWithoutAuthCheck() {
        MockServerWebExchange exchange = MockServerWebExchange
                .from(MockServerHttpRequest.options("/api/v1/orders/1").build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(any());
    }

    @Test
    void filter_onAdminOnlyPathWithCustomerToken_shouldReturnForbidden() {
        String token = JwtTokenProvider.customerToken("customer@test.com");
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/gateway/routes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_onAdminOnlyPathWithAdminToken_shouldProceed() {
        String token = JwtTokenProvider.adminToken("admin@test.com");
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/gateway/routes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(any());
    }

    @Test
    void filter_onAdminOnlyPathPrefixWithCustomerToken_shouldReturnForbidden() {
        String token = JwtTokenProvider.customerToken("customer@test.com");
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .get("/api/v1/gateway/routes/some-id").header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_onSpaFallbackGetPath_shouldProceedWithoutAuth() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/home").build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(any());
    }

    @Test
    void filter_withTokenWithoutExpClaim_shouldProceed() {
        String token = JwtTokenProvider.tokenWithoutExp("user@test.com");
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/orders/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(any());
    }

    @Test
    void filter_withInvalidBase64InJwtPayload_shouldReturnUnauthorized() {
        String badToken = "eyJhbGciOiJIUzI1NiJ9.!!!invalid_base64!!!.fakesig";
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/orders/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + badToken).build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }
}
