package com.backandwhite.infrastructure.filter;

import com.backandwhite.common.constants.AppConstants;
import com.backandwhite.common.security.jwt.JwtProperties;
import com.backandwhite.common.security.jwt.JwtUtils;
import com.backandwhite.common.security.model.TokenClaims;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Filtro global de autenticación JWT. Se ejecuta en todos los requests con
 * orden -100.
 *
 * <p>
 * Flujo:
 * <ol>
 * <li>Si la ruta es pública, delega al siguiente filtro sin validar.</li>
 * <li>Si falta la cabecera Authorization o el prefijo Bearer, retorna 401.</li>
 * <li>Si el token es inválido o expirado, retorna 401.</li>
 * <li>Si el token es válido, propaga los claims como cabeceras al servicio
 * downstream.</li>
 * </ol>
 *
 * <p>
 * Las cabeceras propagadas son definidas en {@link AppConstants}:
 * X-Auth-Subject, X-Auth-Roles, X-Auth-Customer-Id, X-Auth-Employee-Id.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Rutas públicas (cualquier método HTTP).
     */
    private static final List<String> PUBLIC_PATHS = List.of(
            "/nexa-auth",
            "/login",
            "/logout",
            "/oauth2/",
            "/.well-known/",
            "/api/v1/granttypes",
            "/api/v1/auth/",
            "/api/v1/customers/register",
            "/api/v1/customers/login",
            "/api/v1/users/register",
            "/api/v1/users/forgot-password",
            "/api/v1/users/reset-password",
            "/api/v1/users/activate",
            "/login.html",
            "/register.html",
            "/forgot-password.html",
            "/reset-password.html",
            "/reset-success.html",
            "/reset-error.html",
            "/activation-success.html",
            "/activation-error.html",
            "/terms.html",
            "/css/",
            "/js/",
            "/images/",
            "/favicon.ico",
            "/actuator",
            "/swagger-ui",
            "/v3/api-docs",
            "/webjars");

    /**
     * Prefijos de ruta públicos solo para GET (lectura de catálogo, CMS y
     * configuración).
     */
    private static final List<String> PUBLIC_GET_PREFIXES = List.of(
            "/api/v1/products",
            "/api/v1/categories",
            "/api/v1/manufacturers",
            "/api/v1/pages",
            "/api/v1/media/images",
            "/api/v1/config/languages",
            "/api/v1/config/currencies",
            "/api/v1/config/countries",
            "/api/v1/shipping/options");

    private final JwtProperties jwtProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        HttpMethod method = request.getMethod();

        if (isPublicPath(path, method)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.debug("Missing or malformed Authorization header for path: {}", path);
            return writeUnauthorized(exchange);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        Optional<TokenClaims> claimsOpt = JwtUtils.validateAndExtract(token, jwtProperties.secret());

        if (claimsOpt.isEmpty()) {
            log.debug("Invalid or expired JWT for path: {}", path);
            return writeUnauthorized(exchange);
        }

        TokenClaims claims = claimsOpt.get();
        ServerHttpRequest enrichedRequest = buildEnrichedRequest(request, claims);
        return chain.filter(exchange.mutate().request(enrichedRequest).build());
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private boolean isPublicPath(String path, HttpMethod method) {
        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            return true;
        }
        if (HttpMethod.GET.equals(method) && PUBLIC_GET_PREFIXES.stream().anyMatch(path::startsWith)) {
            return true;
        }
        return false;
    }

    private boolean isApiPath(String path) {
        return "/api".equals(path) || path.startsWith("/api/");
    }

    private ServerHttpRequest buildEnrichedRequest(ServerHttpRequest request, TokenClaims claims) {
        var mutate = request.mutate()
                .header(AppConstants.HEADER_AUTH_SUBJECT, claims.subject())
                .header(AppConstants.HEADER_AUTH_ROLES, String.join(",", claims.roles()));

        if (claims.customerId() != null) {
            mutate.header(AppConstants.HEADER_AUTH_CUSTOMER_ID, claims.customerId().toString());
        }
        if (claims.employeeId() != null) {
            mutate.header(AppConstants.HEADER_AUTH_EMPLOYEE_ID, claims.employeeId().toString());
        }
        return mutate.build();
    }

    private Mono<Void> writeUnauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {"code":"AU001","message":"Unauthorized access.","data":null,"details":[],"timestamp":"%s"}
                """.formatted(ZonedDateTime.now()).strip();

        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }
}
