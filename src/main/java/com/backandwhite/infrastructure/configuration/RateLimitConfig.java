package com.backandwhite.infrastructure.configuration;

import java.util.Optional;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import reactor.core.publisher.Mono;

/**
 * Redis-based rate limiter configuration.
 *
 * <p>
 * Token Bucket algorithm:
 * <ul>
 * <li>replenishRate — tokens added per second (sustained rate)</li>
 * <li>burstCapacity — maximum bucket capacity (allowed bursts)</li>
 * <li>requestedTokens — tokens consumed per request</li>
 * </ul>
 *
 * <p>
 * Each route defines its own rate-limit values in the {@code gateway_route}
 * table (columns {@code rate_limit_replenish_rate},
 * {@code rate_limit_burst_capacity}, {@code rate_limit_requested_tokens}).
 * {@link com.backandwhite.infrastructure.repository.PostgresRouteDefinitionRepository}
 * injects a {@code RequestRateLimiter} filter with those per-route values.
 * Routes without rate-limit configuration do not apply throttling.
 *
 * <p>
 * The {@link #redisRateLimiter()} bean defines the default rate limiter values.
 * Per-route values automatically override them.
 */
@Configuration
@Profile("!test")
public class RateLimitConfig {

    /**
     * Default rate limiter: 10 req/min per key.
     *
     * <p>
     * Equivalent configuration:
     * <ul>
     * <li>replenishRate = 1 token/second</li>
     * <li>requestedTokens = 6 tokens/request</li>
     * <li>burstCapacity = 60 tokens (up to 10 requests in a burst)</li>
     * </ul>
     */
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(1, 60, 6);
    }

    /**
     * IP + URL key resolver to throttle each endpoint independently.
     */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            String clientIp = Optional.ofNullable(forwardedFor).map(value -> value.split(",")[0].trim())
                    .filter(value -> !value.isBlank())
                    .orElseGet(() -> Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                            .map(addr -> addr.getAddress().getHostAddress()).orElse("unknown"));

            String path = exchange.getRequest().getURI().getPath();
            return Mono.just(clientIp + ":" + path);
        };
    }

    /**
     * Default KeyResolver for Spring Cloud Gateway's RequestRateLimiter when a
     * route does not explicitly define a {@code key-resolver}.
     *
     * <p>
     * Aligned with {@link #ipKeyResolver()} to avoid empty keys (e.g. when there is
     * no authenticated principal) that could result in 403 responses due to
     * {@code deny-empty-key}.
     */
    @Bean(name = "principalNameKeyResolver")
    public KeyResolver principalNameKeyResolver() {
        return ipKeyResolver();
    }
}
