package com.backandwhite.infrastructure.filter;

import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global traceability filter. Logs method, path, source IP and response code
 * with the duration of each request passing through the gateway. Executes
 * BEFORE the JWT filter (order -200 vs -100).
 */
@Log4j2
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        String requestId = exchange.getRequest().getId();
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getPath().value();
        String remoteIp = Optional.ofNullable(exchange.getRequest().getHeaders().getFirst("X-Forwarded-For"))
                .map(xff -> xff.split(",")[0].trim()).filter(ip -> !ip.isBlank())
                .orElseGet(() -> Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                        .map(addr -> addr.getAddress().getHostAddress()).orElse("unknown"));

        log.info("[{}] --> {} {} from {}", requestId, method, path, remoteIp);

        return chain.filter(exchange).doFinally(signal -> {
            long duration = System.currentTimeMillis() - startTime;
            int status = Optional.ofNullable(exchange.getResponse().getStatusCode()).map(s -> s.value()).orElse(0);
            log.info("[{}] <-- {} {} {} ({}ms)", requestId, method, path, status, duration);
        });
    }

    @Override
    public int getOrder() {
        return -200;
    }
}
