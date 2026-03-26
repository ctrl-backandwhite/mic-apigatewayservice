package com.backandwhite.api.controller;

import com.backandwhite.api.dto.in.RouteDefinitionDtoIn;
import com.backandwhite.api.dto.out.RouteDefinitionDtoOut;
import com.backandwhite.api.mapper.RouteDefinitionDtoMapper;
import com.backandwhite.application.usecase.RouteManagementUseCase;
import com.backandwhite.domain.model.GatewayRoute;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * API de gestión de rutas dinámicas del API Gateway.
 *
 * <p>
 * Permite registrar, actualizar, habilitar/deshabilitar y eliminar rutas
 * en caliente sin necesidad de redeploy. Cada operación de escritura publica
 * un {@code RefreshRoutesEvent} que hace que Spring Cloud Gateway recargue
 * las definiciones desde PostgreSQL de forma inmediata.
 *
 * <p>
 * Acceso restringido al rol {@code ADMIN} o {@code BACKOFFICE} mediante
 * el {@link com.backandwhite.infrastructure.filter.JwtAuthenticationFilter}.
 */
@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/gateway/routes", produces = MediaType.APPLICATION_JSON_VALUE)
public class RouteManagementController {

    private final RouteManagementUseCase routeManagementUseCase;
    private final RouteDefinitionDtoMapper mapper;

    /**
     * Lista todas las rutas registradas (activas e inactivas).
     */
    @GetMapping
    public Flux<RouteDefinitionDtoOut> findAll() {
        return routeManagementUseCase.findAll()
                .map(mapper::toDtoOut);
    }

    /**
     * Obtiene el detalle de una ruta por su identificador.
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<RouteDefinitionDtoOut>> findById(@PathVariable String id) {
        return routeManagementUseCase.findById(id)
                .map(mapper::toDtoOut)
                .map(ResponseEntity::ok);
    }

    /**
     * Registra una nueva ruta en PostgreSQL y recarga el gateway de forma
     * inmediata.
     *
     * <p>
     * Ejemplo de body:
     * 
     * <pre>
     * {
     *   "id": "new-service",
     *   "uri": "http://localhost:9099",
     *   "predicates": ["Path=/api/v1/new/**"],
     *   "filters": [],
     *   "order": 0
     * }(consumes = MediaType.APPLICATION_JSON_VALUE)
     * </pre>
     */
    @PostMapping
    public Mono<ResponseEntity<RouteDefinitionDtoOut>> create(@Valid @RequestBody RouteDefinitionDtoIn dto) {
        return routeManagementUseCase.create(mapper.toDomain(dto))
                .map(mapper::toDtoOut)
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(saved));
    }

    /**
     * Actualiza una ruta existente y recarga el gateway de forma inmediata.
     */
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<RouteDefinitionDtoOut>> update(
            @PathVariable String id,
            @Valid @RequestBody RouteDefinitionDtoIn dto) {
        return routeManagementUseCase.update(mapper.toDomain(dto), id)
                .map(mapper::toDtoOut)
                .map(ResponseEntity::ok);
    }

    /**
     * Elimina una ruta del gateway. La ruta deja de estar activa de forma
     * inmediata.
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable String id) {
        return routeManagementUseCase.delete(id)
                .then(Mono.just(ResponseEntity.<Void>noContent().build()));
    }

    /**
     * Elimina múltiples rutas en una sola operación.
     * Las rutas que no existan se omiten silenciosamente.
     *
     * <pre>
     * DELETE /api/v1/gateway/routes/bulk
     * { "ids": ["route-1", "route-2", "route-3"] }
     * </pre>
     *
     * @return cantidad de rutas efectivamente eliminadas.
     */
    @DeleteMapping("/bulk")
    public Mono<ResponseEntity<Map<String, Long>>> bulkDelete(@RequestBody Map<String, List<String>> body) {
        List<String> ids = body.getOrDefault("ids", List.of());
        return routeManagementUseCase.bulkDelete(ids)
                .map(deleted -> ResponseEntity.ok(Map.of("deleted", deleted)));
    }

    /**
     * Importa múltiples rutas de forma masiva. Las rutas cuyo id ya exista
     * se omiten y se informan en el resultado.
     *
     * <pre>
     * POST /api/v1/gateway/routes/bulk
     * [
     *   { "id": "svc-1", "uri": "http://localhost:8080", "predicates": ["Path=/api/v1/svc1/**"], "filters": [], "order": 0 },
     *   { "id": "svc-2", "uri": "http://localhost:9090", "predicates": ["Path=/api/v1/svc2/**"], "filters": [], "order": 1 }
     * ]
     * </pre>
     */
    @PostMapping("/bulk")
    public Mono<ResponseEntity<Map<String, Object>>> bulkImport(
            @RequestBody List<@Valid RouteDefinitionDtoIn> dtos) {
        List<GatewayRoute> routes = dtos.stream()
                .map(mapper::toDomain)
                .toList();
        return routeManagementUseCase.bulkImport(routes)
                .map(ResponseEntity::ok);
    }

    /**
     * Activa o desactiva una ruta sin eliminarla. Útil para mantenimiento.
     */
    @PatchMapping("/{id}/toggle")
    public Mono<ResponseEntity<RouteDefinitionDtoOut>> toggle(@PathVariable String id) {
        return routeManagementUseCase.toggleEnabled(id)
                .map(mapper::toDtoOut)
                .map(ResponseEntity::ok);
    }

    /**
     * Fuerza el refresco de rutas en el gateway sin modificar datos.
     * Útil tras cambios manuales en la base de datos.
     */
    @PostMapping("/refresh")
    public Mono<ResponseEntity<Void>> refresh() {
        return routeManagementUseCase.refresh()
                .then(Mono.just(ResponseEntity.<Void>ok().build()));
    }
}
