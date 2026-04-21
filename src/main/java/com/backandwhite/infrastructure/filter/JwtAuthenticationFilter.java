package com.backandwhite.infrastructure.filter;

import com.backandwhite.common.constants.AppConstants;
import com.backandwhite.common.security.model.TokenClaims;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
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
            "/api/v1/public/", "/api/v1/reviews", "/api/v1/brands", "/api/v1/slides/active",
            "/api/v1/gift-cards/designs/active", "/api/v1/loyalty/tiers", "/api/v1/loyalty/rules",
            "/api/v1/currency-rates", "/api/v1/settings", "/api/v1/campaigns", "/api/v1/seo",
            // Signed-URL PDF invoices — the sig+exp query params carry the
            // auth; the endpoint validates HMAC server-side.
            "/api/v1/invoices/public/");

    /**
     * Paths that must be accessible ONLY to ADMIN or BACKOFFICE. Evaluated
     * before the generic token-presence check so that anonymous access is
     * rejected even when a path would otherwise pass through.
     */
    private static final List<String> ADMIN_ONLY_PATHS = List.of("/api/v1/gateway/routes");

    private static final List<String> ADMIN_ROLES = List.of("ROLE_ADMIN", "ADMIN", "ROLE_BACKOFFICE", "BACKOFFICE");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        HttpMethod method = request.getMethod();

        // CORS preflights never carry Authorization and must never be blocked
        // by this filter — the CorsWebFilter further down the chain is
        // responsible for answering them with the right headers.
        if (HttpMethod.OPTIONS.equals(method)) {
            return chain.filter(exchange);
        }

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

        // Parse claims for downstream enrichment but do NOT validate the signature:
        // Spring Authorization Server issues RSA-signed JWTs that can't be verified
        // with the gateway's HMAC secret. Signature validation is performed by each
        // resource server via its oauth2ResourceServer JWT decoder.
        String token = authHeader.substring(BEARER_PREFIX.length());
        Optional<TokenClaims> claimsOpt = parseClaimsUnsafely(token);

        if (isAdminOnlyPath(path) && !hasAdminRole(claimsOpt)) {
            return forbidden(exchange);
        }

        claimsOpt.ifPresent(claims -> enrichWithClaims(mutate, claims));
        return chain.filter(exchange.mutate().request(mutate.build()).build());
    }

    private boolean isAdminOnlyPath(String path) {
        for (String adminPath : ADMIN_ONLY_PATHS) {
            if (path.equals(adminPath) || path.startsWith(adminPath)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAdminRole(Optional<TokenClaims> claimsOpt) {
        if (claimsOpt.isEmpty()) {
            return false;
        }
        List<String> roles = claimsOpt.get().roles();
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        for (String role : roles) {
            if (ADMIN_ROLES.contains(role)) {
                return true;
            }
        }
        return false;
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
            parseClaimsUnsafely(token).ifPresent(claims -> enrichWithClaims(mutate, claims));
        }
    }

    /**
     * Decodes the JWT payload without verifying the signature. The downstream
     * resource server is responsible for signature validation via its JWK Set URI;
     * this method only exposes claims so the gateway can propagate them as HTTP
     * headers for routing, rate-limiting and logging.
     */
    private Optional<TokenClaims> parseClaimsUnsafely(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return Optional.empty();
            }
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode payload = MAPPER.readTree(new String(payloadBytes, StandardCharsets.UTF_8));

            String subject = payload.path("sub").asText(null);
            String email = payload.path("email").asText(null);

            List<String> roles = new ArrayList<>();
            JsonNode rolesNode = payload.path("roles");
            if (rolesNode.isArray()) {
                rolesNode.forEach(n -> roles.add(n.asText()));
            }

            Long customerId = payload.hasNonNull("customerId") ? payload.get("customerId").asLong() : null;
            Long employeeId = payload.hasNonNull("employeeId") ? payload.get("employeeId").asLong() : null;

            return Optional.of(new TokenClaims(subject, email, roles, customerId, employeeId));
        } catch (Exception e) { // NOSONAR java:S1166 — intentionally lenient
            log.debug("Could not decode JWT payload: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private void enrichWithClaims(ServerHttpRequest.Builder mutate, TokenClaims claims) {
        mutate.header(AppConstants.HEADER_AUTH_SUBJECT, claims.subject()).header(AppConstants.HEADER_AUTH_ROLES,
                String.join(",", claims.roles()));

        if (claims.email() != null && !claims.email().isBlank()) {
            mutate.header(AppConstants.HEADER_AUTH_EMAIL, claims.email());
        }
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

    private Mono<Void> forbidden(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }
}
