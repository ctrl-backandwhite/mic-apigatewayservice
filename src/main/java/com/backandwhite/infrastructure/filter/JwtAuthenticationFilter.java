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
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * Filtro global del Gateway. Todas las rutas son públicas.
 * Si el request incluye un JWT válido en Authorization, extrae los claims
 * y los propaga como cabeceras HTTP a los microservicios downstream.
 * Si no hay JWT o es inválido, el request pasa igualmente sin cabeceras de
 * usuario.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProperties jwtProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Inyectar cabecera gateway→servicio en TODAS las peticiones.
        // Eliminar Origin para evitar que los servicios downstream apliquen
        // su propio filtro CORS: la validación CORS se gestiona únicamente
        // en el gateway.
        ServerHttpRequest.Builder mutate = request.mutate()
                .header(AppConstants.HEADER_NX036_AUTH, "gateway")
                .headers(h -> h.remove(HttpHeaders.ORIGIN));

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            Optional<TokenClaims> claimsOpt = JwtUtils.validateAndExtract(token, jwtProperties.secret());
            if (claimsOpt.isPresent()) {
                enrichWithClaims(mutate, claimsOpt.get());
            }
        }

        return chain.filter(exchange.mutate().request(mutate.build()).build());
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private void enrichWithClaims(ServerHttpRequest.Builder mutate, TokenClaims claims) {
        mutate.header(AppConstants.HEADER_AUTH_SUBJECT, claims.subject())
                .header(AppConstants.HEADER_AUTH_ROLES, String.join(",", claims.roles()));

        if (claims.email() != null) {
            mutate.header(AppConstants.HEADER_AUTH_EMAIL, claims.email());
        } else if (claims.subject() != null) {
            // fallback: subject is the user's email for password-based logins
            mutate.header(AppConstants.HEADER_AUTH_EMAIL, claims.subject());
        }
        if (claims.customerId() != null) {
            mutate.header(AppConstants.HEADER_AUTH_CUSTOMER_ID, claims.customerId().toString());
        }
        if (claims.employeeId() != null) {
            mutate.header(AppConstants.HEADER_AUTH_EMPLOYEE_ID, claims.employeeId().toString());
        }
    }
}
