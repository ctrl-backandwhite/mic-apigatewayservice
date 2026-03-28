package com.backandwhite.api.handler;

import com.backandwhite.api.dto.GatewayErrorResponseDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Manejador global de excepciones a nivel de enrutamiento del Gateway
 * (WebFlux).
 *
 * <p>
 * Al estar en {@code @Order(-2)}, se ejecuta antes de
 * {@code DefaultErrorWebExceptionHandler} (orden -1), interceptando tanto
 * errores de enrutamiento (404 sin ruta coincidente) como cualquier otra
 * excepción no capturada por {@link GatewayExceptionHandler}.
 *
 * <ul>
 * <li>Si el cliente acepta {@code text/html}, devuelve una página HTML
 * autocontenida con el diseño de NX036 (sin dependencias externas).</li>
 * <li>En cualquier otro caso, devuelve {@link GatewayErrorResponseDto}
 * JSON.</li>
 * <li>Añade cabecera {@code Access-Control-Allow-Origin} cuando el origen
 * de la petición está en la lista de orígenes permitidos.</li>
 * </ul>
 *
 * @author NX036-ALFA
 */
@Log4j2
@Component
@Order(-2)
@RequiredArgsConstructor
public class GatewayWebExceptionHandler implements WebExceptionHandler {

    private final ObjectMapper objectMapper;

    private static final List<String> ALLOWED_ORIGINS = List.of(
            "http://localhost:4200",
            "http://localhost:9000",
            "https://web-auth-des.up.railway.app",
            "https://gateway-service-des.up.railway.app",
            "https://nx036.com");

    // -------------------------------------------------------------------------
    // Handle
    // -------------------------------------------------------------------------

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status = resolveStatus(ex);
        String message = resolveMessage(ex, status);
        String code = "GW" + status.value();

        log.warn("Gateway error [{}] {}: {}", status.value(), code, ex.getMessage());

        applyCorsHeaders(exchange);
        exchange.getResponse().setStatusCode(status);

        String accept = exchange.getRequest().getHeaders().getFirst(HttpHeaders.ACCEPT);
        // Return HTML unless the client explicitly requests JSON only.
        // Browsers send 'text/html,...' or '*/*'; API clients send 'application/json'.
        boolean wantsJson = accept != null
                && accept.contains("application/json")
                && !accept.contains("text/html");

        return !wantsJson
                ? writeHtml(exchange, status.value(), message)
                : writeJson(exchange, code, message);
    }

    // -------------------------------------------------------------------------
    // Status + message resolution
    // -------------------------------------------------------------------------

    private HttpStatus resolveStatus(Throwable ex) {
        if (ex instanceof ResponseStatusException rse) {
            HttpStatus resolved = HttpStatus.resolve(rse.getStatusCode().value());
            return resolved != null ? resolved : HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String resolveMessage(Throwable ex, HttpStatus status) {
        if (ex instanceof ResponseStatusException rse && rse.getReason() != null
                && !rse.getReason().isBlank()) {
            return rse.getReason();
        }
        return switch (status) {
            case NOT_FOUND -> "No se encontró el recurso solicitado.";
            case UNAUTHORIZED -> "No autorizado. Por favor, inicia sesión.";
            case FORBIDDEN -> "Acceso denegado. No tienes permisos suficientes.";
            case BAD_REQUEST -> "Solicitud incorrecta.";
            case METHOD_NOT_ALLOWED -> "Método HTTP no permitido.";
            case TOO_MANY_REQUESTS -> "Demasiadas solicitudes. Por favor, espera un momento.";
            case SERVICE_UNAVAILABLE -> "Servicio temporalmente no disponible.";
            default -> "Ha ocurrido un error inesperado.";
        };
    }

    // -------------------------------------------------------------------------
    // CORS
    // -------------------------------------------------------------------------

    private void applyCorsHeaders(ServerWebExchange exchange) {
        String origin = exchange.getRequest().getHeaders().getFirst(HttpHeaders.ORIGIN);
        if (origin == null)
            return;
        boolean allowed = ALLOWED_ORIGINS.stream()
                .anyMatch(o -> o.equalsIgnoreCase(origin));
        if (allowed) {
            HttpHeaders headers = exchange.getResponse().getHeaders();
            headers.set("Access-Control-Allow-Origin", origin);
            headers.set("Access-Control-Allow-Credentials", "true");
        }
    }

    // -------------------------------------------------------------------------
    // Writers
    // -------------------------------------------------------------------------

    private Mono<Void> writeJson(ServerWebExchange exchange, String code, String message) {
        GatewayErrorResponseDto dto = GatewayErrorResponseDto.builder()
                .code(code)
                .message(message)
                .details(List.of())
                .dateTime(ZonedDateTime.now())
                .build();

        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(dto);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Error serializing error response", e);
            return Mono.error(e);
        }
    }

    private Mono<Void> writeHtml(ServerWebExchange exchange, int statusCode, String message) {
        exchange.getResponse().getHeaders().setContentType(
                new MediaType("text", "html", StandardCharsets.UTF_8));
        String html = buildHtmlPage(statusCode, escapeHtml(message));
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    // -------------------------------------------------------------------------
    // HTML page
    // -------------------------------------------------------------------------

    private String buildHtmlPage(int statusCode, String message) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>NX036 · %1$d</title>
                  <style>
                    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
                    html, body { height: 100%%; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0f172a; color: #f1f5f9; }
                    .page { display: flex; flex-direction: column; min-height: 100vh; }
                    header { display: flex; align-items: center; padding: 1.25rem 2rem; border-bottom: 1px solid #1e293b; }
                    .logo { font-size: 1.5rem; font-weight: 800; letter-spacing: .15em; color: #38bdf8; text-decoration: none; }
                    main { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 3rem 1.5rem; text-align: center; }
                    .code { font-size: 7rem; font-weight: 900; line-height: 1; color: #38bdf8; letter-spacing: -.05em; }
                    .divider { width: 4rem; height: 3px; background: #334155; margin: 1.5rem auto; border-radius: 2px; }
                    .message { font-size: 1.25rem; color: #94a3b8; max-width: 36rem; line-height: 1.6; }
                    .btn { display: inline-block; margin-top: 2.5rem; padding: .75rem 2rem; border: 2px solid #38bdf8; color: #38bdf8; border-radius: .5rem; text-decoration: none; font-size: 1rem; font-weight: 600; letter-spacing: .03em; transition: background .2s, color .2s; }
                    .btn:hover { background: #38bdf8; color: #0f172a; }
                    footer { text-align: center; padding: 1.25rem; font-size: .8rem; color: #475569; border-top: 1px solid #1e293b; }
                  </style>
                </head>
                <body>
                  <div class="page">
                    <header><a href="/" class="logo">NX036</a></header>
                    <main>
                      <div class="code">%1$d</div>
                      <div class="divider"></div>
                      <p class="message">%2$s</p>
                      <a href="/" class="btn">Volver al inicio</a>
                    </main>
                    <footer>&copy; 2025 NX036 &mdash; Todos los derechos reservados</footer>
                  </div>
                </body>
                </html>
                """
                .formatted(statusCode, message);
    }

    private String escapeHtml(String input) {
        if (input == null)
            return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
