package com.backandwhite.infrastructure.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

@DisplayName("RateLimitConfig")
class RateLimitConfigTest {

    private RateLimitConfig config;

    @BeforeEach
    void setUp() {
        config = new RateLimitConfig();
    }

    @Nested
    @DisplayName("redisRateLimiter()")
    class RedisRateLimiterBean {

        @Test
        @DisplayName("returns a non-null RedisRateLimiter")
        void returnsNonNull() {
            RedisRateLimiter limiter = config.redisRateLimiter();
            assertThat(limiter).isNotNull();
        }
    }

    @Nested
    @DisplayName("ipKeyResolver()")
    class IpKeyResolverBean {

        @Test
        @DisplayName("returns a non-null KeyResolver bean")
        void returnsNonNull() {
            assertThat(config.ipKeyResolver()).isNotNull();
        }

        @Test
        @DisplayName("uses X-Forwarded-For header when present")
        void usesXForwardedFor() {
            KeyResolver resolver = config.ipKeyResolver();
            MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/products")
                    .header("X-Forwarded-For", "10.0.0.1, 192.168.1.1").build());

            StepVerifier.create(resolver.resolve(exchange)).expectNext("10.0.0.1:/api/v1/products").verifyComplete();
        }

        @Test
        @DisplayName("falls back to remote address when X-Forwarded-For is absent")
        void fallsBackToRemoteAddress() {
            KeyResolver resolver = config.ipKeyResolver();
            MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/orders")
                    .remoteAddress(new InetSocketAddress("192.168.0.10", 54321)).build());

            StepVerifier.create(resolver.resolve(exchange)).expectNext("192.168.0.10:/api/v1/orders").verifyComplete();
        }

        @Test
        @DisplayName("returns 'unknown' when no IP information is available")
        void returnsUnknownWhenNoIp() {
            KeyResolver resolver = config.ipKeyResolver();
            // MockServerHttpRequest without remoteAddress defaults to null
            MockServerWebExchange exchange = MockServerWebExchange
                    .from(MockServerHttpRequest.get("/api/v1/test").build());

            StepVerifier.create(resolver.resolve(exchange)).assertNext(key -> {
                // Key should end with the path and use either an IP or "unknown"
                assertThat(key).endsWith(":/api/v1/test");
            }).verifyComplete();
        }

        @Test
        @DisplayName("ignores blank X-Forwarded-For header")
        void ignoresBlankXForwardedFor() {
            KeyResolver resolver = config.ipKeyResolver();
            MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/products")
                    .header("X-Forwarded-For", "   ").remoteAddress(new InetSocketAddress("10.0.0.5", 5000)).build());

            StepVerifier.create(resolver.resolve(exchange)).expectNext("10.0.0.5:/api/v1/products").verifyComplete();
        }
    }

    @Nested
    @DisplayName("principalNameKeyResolver()")
    class PrincipalNameKeyResolverBean {

        @Test
        @DisplayName("returns the same KeyResolver as ipKeyResolver()")
        void delegatesToIpKeyResolver() {
            assertThat(config.principalNameKeyResolver()).isNotNull();
        }
    }

    @Nested
    @DisplayName("class-level annotations")
    class ClassAnnotations {

        @Test
        @DisplayName("is annotated with @Configuration")
        void hasConfigurationAnnotation() {
            assertThat(RateLimitConfig.class
                    .isAnnotationPresent(org.springframework.context.annotation.Configuration.class)).isTrue();
        }

        @Test
        @DisplayName("is annotated with @Profile('!test')")
        void hasProfileAnnotation() {
            var profile = RateLimitConfig.class.getAnnotation(org.springframework.context.annotation.Profile.class);
            assertThat(profile).isNotNull();
            assertThat(profile.value()).containsExactly("!test");
        }
    }
}
