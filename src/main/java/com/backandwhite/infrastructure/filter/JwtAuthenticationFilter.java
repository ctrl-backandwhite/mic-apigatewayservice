package com.backandwhite.infrastructure.filter;

import com.backandwhite.common.constants.AppConstants;
import com.backandwhite.common.security.jwt.JwtProperties;
import com.backandwhite.common.security.jwt.JwtUtils;
import com.backandwhite.common.security.model.TokenClaims;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Log4j2
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";

    private static final List<String> PUBLIC_PATHS = List.of("/oauth2/", "/.well-known/", "/login", "/logout",
            "/register.html", "/forgot-password.html", "/reset-password.html", "/reset-success.html",
            "/reset-error.html", "/activation-success.html", "/activation-error.html", "/terms.html", "/css/", "/js/",
            "/images/", "/favicon.ico", "/actuator/", "/nexa-auth/", "/api/v1/auth/login", "/api/v1/auth/register",
            "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password", "/api/v1/auth/activate",
            "/api/v1/auth/refresh-token", "/api/v1/cj/webhook/");

    private static final List<String> PUBLIC_GET_PATHS = List.of("/api/v1/products", "/api/v1/categories",
            "/api/v1/public/", "/api/v1/reviews", "/api/v1/brands");

    private final JwtProperties jwtProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        HttpMethod method = request.getMethod();

        ServerHttpRequest.Builder mutate = request.mutate().header(AppConstants.HEADER_NX036_AUTH, "gateway")
                .headers(h -> h.remove(HttpHeaders.ORIGIN));

        if (isPublicPath(path, method)) {
            enrichIfTokenPresent(request, mutate);
            return chain.filter(exchange.mutate().request(mutate.build()).build());
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        Optional<TokenClaims> claimsOpt = JwtUtils.validateAndExtract(token, jwtProperties.secret());
        if (claimsOpt.isEmpty()) {
            return unauthorized(exchange);
        }

        enrichWithClaims(mutate, claimsOpt.get());
        return chain.filter(exchange.mutate().request(mutate.build()).build());
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private boolean isPublicPath(String path, HttpMethod method) {
        for (String publicPath : PUBLIC_PATHS) {
            if (path.equals(publicPath) || path.startsWith(publicPath)) {
                return true;
            }
        }
        if (HttpMethod.GET.equals(method)) {
            for (String getPath : PUBLIC_GET_PATHS) {
                if (path.equals(getPath) || path.startsWith(getPath)) {
                    return true;
                }
            }
            // SPA fallback: any non-API GET is a frontend asset (HTML, JS chunks,
            // images, Vite HMR, React refresh, etc.) routed to the Vite dev server
            // or Caddy in prod via the ecomerce-frontend route.
            if (!path.startsWith("/api/v1/")) {
                return true;
            }
        }
        return false;
    }

    private void enrichIfTokenPresent(ServerHttpRequest request, ServerHttpRequest.Builder mutate) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            JwtUtils.validateAndExtract(token, jwtProperties.secret())
                    .ifPresent(claims -> enrichWithClaims(mutate, claims));
        }
    }

    private void enrichWithClaims(ServerHttpRequest.Builder mutate, TokenClaims claims) {
        mutate.header(AppConstants.HEADER_AUTH_SUBJECT, claims.subject()).header(AppConstants.HEADER_AUTH_ROLES,
                String.join(",", claims.roles()));

        if (claims.customerId() != null) {
            mutate.header(AppConstants.HEADER_AUTH_CUSTOMER_ID, claims.customerId().toString());
        }
        if (claims.employeeId() != null) {
            mutate.header(AppConstants.HEADER_AUTH_EMPLOYEE_ID, claims.employeeId().toString());
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
