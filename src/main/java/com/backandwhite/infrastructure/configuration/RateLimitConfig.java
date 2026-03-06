package com.backandwhite.infrastructure.configuration;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * Configuración del rate limiter basado en Redis.
 *
 * <p>Algoritmo Token Bucket:
 * <ul>
 *   <li>replenishRate  — tokens añadidos por segundo (velocidad sostenida)</li>
 *   <li>burstCapacity  — capacidad máxima del bucket (picos permitidos)</li>
 *   <li>requestedTokens — tokens consumidos por request</li>
 * </ul>
 *
 * <p>Los valores se pueden sobreescribir por ruta en application.yml mediante:
 * <pre>
 * filters:
 *   - name: RequestRateLimiter
 *     args:
 *       redis-rate-limiter.replenishRate: 20
 *       redis-rate-limiter.burstCapacity: 40
 * </pre>
 */
@Configuration
@Profile("!test")
public class RateLimitConfig {

    /**
     * Rate limiter por defecto: 10 req/s, burst hasta 20, 1 token por request.
     */
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(10, 20, 1);
    }

    /**
     * Resolver de clave basado en IP del cliente para identificar al consumidor.
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
                Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                        .map(addr -> addr.getAddress().getHostAddress())
                        .orElse("unknown")
        );
    }
}
