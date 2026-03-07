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
 * <p>
 * Algoritmo Token Bucket:
 * <ul>
 * <li>replenishRate — tokens añadidos por segundo (velocidad sostenida)</li>
 * <li>burstCapacity — capacidad máxima del bucket (picos permitidos)</li>
 * <li>requestedTokens — tokens consumidos por request</li>
 * </ul>
 *
 * <p>
 * Los valores se pueden sobreescribir por ruta en application.yml mediante:
 * 
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
     * Rate limiter por defecto: 10 req/min por clave.
     *
     * <p>
     * Configuración equivalente:
     * <ul>
     * <li>replenishRate = 1 token/segundo</li>
     * <li>requestedTokens = 6 tokens/request</li>
     * <li>burstCapacity = 60 tokens (hasta 10 requests en ráfaga)</li>
     * </ul>
     */
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(1, 60, 6);
    }

    /**
     * Resolver de clave por IP + URL para limitar cada endpoint de forma
     * independiente.
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            String clientIp = Optional.ofNullable(forwardedFor)
                    .map(value -> value.split(",")[0].trim())
                    .filter(value -> !value.isBlank())
                    .orElseGet(() -> Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                            .map(addr -> addr.getAddress().getHostAddress())
                            .orElse("unknown"));

            String path = exchange.getRequest().getURI().getPath();
            return Mono.just(clientIp + ":" + path);
        };
    }

    /**
     * KeyResolver por defecto de Spring Cloud Gateway para RequestRateLimiter
     * cuando una ruta no define explícitamente {@code key-resolver}.
     *
     * <p>
     * Se alinea con {@link #ipKeyResolver()} para evitar claves vacías (por
     * ejemplo, cuando no hay principal autenticado) que podrían terminar en
     * respuestas 403 por {@code deny-empty-key}.
     */
    @Bean(name = "principalNameKeyResolver")
    public KeyResolver principalNameKeyResolver() {
        return ipKeyResolver();
    }
}
