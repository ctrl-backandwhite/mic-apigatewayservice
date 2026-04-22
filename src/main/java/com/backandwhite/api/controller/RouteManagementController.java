package com.backandwhite.api.controller;

import com.backandwhite.api.dto.in.RouteDefinitionDtoIn;
import com.backandwhite.api.dto.out.RouteDefinitionDtoOut;
import com.backandwhite.api.mapper.RouteDefinitionDtoMapper;
import com.backandwhite.application.usecase.RouteManagementUseCase;
import com.backandwhite.domain.model.GatewayRoute;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

/**
 * Dynamic route management API for the API Gateway.
 *
 * <p>
 * Allows registering, updating, enabling/disabling and deleting routes on the
 * fly without redeploying. Each write operation publishes a
 * {@code RefreshRoutesEvent} that makes Spring Cloud Gateway reload definitions
 * from PostgreSQL immediately.
 *
 * <p>
 * Access restricted to the {@code ADMIN} or {@code BACKOFFICE} role via the
 * {@link com.backandwhite.infrastructure.filter.JwtAuthenticationFilter}.
 */
@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/gateway/routes", produces = MediaType.APPLICATION_JSON_VALUE)
public class RouteManagementController {

    private final RouteManagementUseCase routeManagementUseCase;
    private final RouteDefinitionDtoMapper mapper;

    /**
     * Lists all registered routes (active and inactive).
     */
    @GetMapping
    public Flux<RouteDefinitionDtoOut> findAll() {
        return routeManagementUseCase.findAll().map(mapper::toDtoOut);
    }

    /**
     * Gets the detail of a route by its identifier.
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<RouteDefinitionDtoOut>> findById(@PathVariable String id) {
        return routeManagementUseCase.findById(id).map(mapper::toDtoOut).map(ResponseEntity::ok);
    }

    /**
     * Registers a new route in PostgreSQL and reloads the gateway immediately.
     *
     * <p>
     * Example body:
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
        return routeManagementUseCase.create(mapper.toDomain(dto)).map(mapper::toDtoOut)
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(saved));
    }

    /**
     * Updates an existing route and reloads the gateway immediately.
     */
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<RouteDefinitionDtoOut>> update(@PathVariable String id,
            @Valid @RequestBody RouteDefinitionDtoIn dto) {
        return routeManagementUseCase.update(mapper.toDomain(dto), id).map(mapper::toDtoOut).map(ResponseEntity::ok);
    }

    /**
     * Deletes a gateway route. The route becomes inactive immediately.
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable String id) {
        return routeManagementUseCase.delete(id).then(Mono.just(ResponseEntity.<Void>noContent().build()));
    }

    /**
     * Deletes multiple routes in a single operation. Non-existent routes are
     * silently skipped.
     *
     * <pre>
     * DELETE /api/v1/gateway/routes/bulk
     * { "ids": ["route-1", "route-2", "route-3"] }
     * </pre>
     *
     * @return number of routes effectively deleted.
     */
    @DeleteMapping("/bulk")
    public Mono<ResponseEntity<Map<String, Long>>> bulkDelete(@RequestBody Map<String, List<String>> body) {
        List<String> ids = body.getOrDefault("ids", List.of());
        return routeManagementUseCase.bulkDelete(ids).map(deleted -> ResponseEntity.ok(Map.of("deleted", deleted)));
    }

    /**
     * Bulk-imports multiple routes. Routes whose id already exists are skipped and
     * reported in the result.
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
    public Mono<ResponseEntity<Map<String, Object>>> bulkImport(@RequestBody List<@Valid RouteDefinitionDtoIn> dtos) {
        List<GatewayRoute> routes = dtos.stream().map(mapper::toDomain).toList();
        return routeManagementUseCase.bulkImport(routes).map(ResponseEntity::ok);
    }

    /**
     * Toggles a route on or off without deleting it. Useful for maintenance.
     */
    @PatchMapping("/{id}/toggle")
    public Mono<ResponseEntity<RouteDefinitionDtoOut>> toggle(@PathVariable String id) {
        return routeManagementUseCase.toggleEnabled(id).map(mapper::toDtoOut).map(ResponseEntity::ok);
    }

    /**
     * Forces a route refresh in the gateway without modifying data. Useful after
     * manual database changes.
     */
    @PostMapping("/refresh")
    public Mono<ResponseEntity<Void>> refresh() {
        return routeManagementUseCase.refresh().then(Mono.just(ResponseEntity.<Void>ok().build()));
    }
}
