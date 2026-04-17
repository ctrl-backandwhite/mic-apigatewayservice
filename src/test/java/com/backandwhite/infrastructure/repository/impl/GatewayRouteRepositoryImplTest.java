package com.backandwhite.infrastructure.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.domain.model.GatewayRoute;
import com.backandwhite.infrastructure.entity.GatewayRouteEntity;
import com.backandwhite.infrastructure.mapper.GatewayRouteEntityMapper;
import com.backandwhite.infrastructure.repository.GatewayRouteR2dbcRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class GatewayRouteRepositoryImplTest {

    @Mock
    private GatewayRouteR2dbcRepository r2dbcRepository;

    @Mock
    private GatewayRouteEntityMapper entityMapper;

    @Mock
    private R2dbcEntityTemplate r2dbcEntityTemplate;

    @Mock
    private DatabaseClient databaseClient;

    @InjectMocks
    private GatewayRouteRepositoryImpl repository;

    private static GatewayRoute sampleDomain() {
        return GatewayRoute.builder().id("catalog-service").uri("http://localhost:8083")
                .predicates(List.of("Path=/api/v1/products/**")).filters(List.of()).order(0).enabled(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    private static GatewayRouteEntity sampleEntity() {
        return GatewayRouteEntity.builder().id("catalog-service").uri("http://localhost:8083")
                .predicates("[\"Path=/api/v1/products/**\"]").filters("[]").order(0).enabled(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    // ------------------------------------------------------------------
    // findAll
    // ------------------------------------------------------------------

    @Test
    void findAll_shouldDelegateToR2dbcRepository() {
        GatewayRouteEntity entity = sampleEntity();
        GatewayRoute domain = sampleDomain();
        when(r2dbcRepository.findAll()).thenReturn(Flux.just(entity));
        when(entityMapper.toDomain(entity)).thenReturn(domain);

        StepVerifier.create(repository.findAll()).assertNext(r -> assertThat(r.getId()).isEqualTo("catalog-service"))
                .verifyComplete();

        verify(r2dbcRepository).findAll();
    }

    @Test
    void findAll_whenEmpty_shouldReturnEmptyFlux() {
        when(r2dbcRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(repository.findAll()).verifyComplete();
    }

    // ------------------------------------------------------------------
    // findAllEnabled
    // ------------------------------------------------------------------

    @Test
    void findAllEnabled_shouldReturnOnlyEnabledRoutes() {
        GatewayRouteEntity entity = sampleEntity();
        GatewayRoute domain = sampleDomain();
        when(r2dbcRepository.findByEnabledTrue()).thenReturn(Flux.just(entity));
        when(entityMapper.toDomain(entity)).thenReturn(domain);

        StepVerifier.create(repository.findAllEnabled()).assertNext(r -> assertThat(r.isEnabled()).isTrue())
                .verifyComplete();

        verify(r2dbcRepository).findByEnabledTrue();
    }

    // ------------------------------------------------------------------
    // findById
    // ------------------------------------------------------------------

    @Test
    void findById_whenFound_shouldReturnMappedDomain() {
        GatewayRouteEntity entity = sampleEntity();
        GatewayRoute domain = sampleDomain();
        when(r2dbcRepository.findById("catalog-service")).thenReturn(Mono.just(entity));
        when(entityMapper.toDomain(entity)).thenReturn(domain);

        StepVerifier.create(repository.findById("catalog-service"))
                .assertNext(r -> assertThat(r.getId()).isEqualTo("catalog-service")).verifyComplete();
    }

    @Test
    void findById_whenNotFound_shouldReturnEmpty() {
        when(r2dbcRepository.findById("non-existent")).thenReturn(Mono.empty());

        StepVerifier.create(repository.findById("non-existent")).verifyComplete();
    }

    // ------------------------------------------------------------------
    // save
    // ------------------------------------------------------------------

    @Test
    void save_shouldInsertViaTemplate() {
        GatewayRoute domain = sampleDomain();
        GatewayRouteEntity entity = sampleEntity();
        when(entityMapper.toEntity(domain)).thenReturn(entity);
        when(r2dbcEntityTemplate.insert(any(GatewayRouteEntity.class))).thenReturn(Mono.just(entity));
        when(entityMapper.toDomain(entity)).thenReturn(domain);

        StepVerifier.create(repository.save(domain))
                .assertNext(saved -> assertThat(saved.getId()).isEqualTo("catalog-service")).verifyComplete();

        verify(r2dbcEntityTemplate).insert(any(GatewayRouteEntity.class));
    }

    // ------------------------------------------------------------------
    // update
    // ------------------------------------------------------------------

    @Test
    void update_whenRouteExists_shouldMergeAndSave() {
        GatewayRouteEntity existing = sampleEntity();
        GatewayRoute updatedDomain = sampleDomain();
        GatewayRouteEntity updatedEntity = sampleEntity();
        updatedEntity.setUri("http://localhost:9090");

        when(r2dbcRepository.findById("catalog-service")).thenReturn(Mono.just(existing));
        when(entityMapper.toEntity(updatedDomain)).thenReturn(updatedEntity);
        when(r2dbcRepository.save(any(GatewayRouteEntity.class))).thenReturn(Mono.just(updatedEntity));
        when(entityMapper.toDomain(updatedEntity)).thenReturn(updatedDomain);

        StepVerifier.create(repository.update(updatedDomain, "catalog-service"))
                .assertNext(r -> assertThat(r.getId()).isEqualTo("catalog-service")).verifyComplete();

        verify(r2dbcRepository).save(any(GatewayRouteEntity.class));
    }

    @Test
    void update_whenNotFound_shouldReturnEmpty() {
        when(r2dbcRepository.findById("non-existent")).thenReturn(Mono.empty());

        StepVerifier.create(repository.update(sampleDomain(), "non-existent")).verifyComplete();
    }

    // ------------------------------------------------------------------
    // delete
    // ------------------------------------------------------------------

    @Test
    void delete_shouldDelegateToR2dbcRepository() {
        when(r2dbcRepository.deleteById("catalog-service")).thenReturn(Mono.empty());

        StepVerifier.create(repository.delete("catalog-service")).verifyComplete();

        verify(r2dbcRepository).deleteById("catalog-service");
    }

    // ------------------------------------------------------------------
    // toggleEnabled
    // ------------------------------------------------------------------

    @Test
    void toggleEnabled_shouldFlipEnabledFlag() {
        GatewayRouteEntity entity = sampleEntity();
        entity.setEnabled(true);
        GatewayRouteEntity toggled = sampleEntity();
        toggled.setEnabled(false);
        GatewayRoute domain = sampleDomain().withEnabled(false);

        when(r2dbcRepository.findById("catalog-service")).thenReturn(Mono.just(entity));
        when(r2dbcRepository.save(any(GatewayRouteEntity.class))).thenReturn(Mono.just(toggled));
        when(entityMapper.toDomain(toggled)).thenReturn(domain);

        StepVerifier.create(repository.toggleEnabled("catalog-service"))
                .assertNext(r -> assertThat(r.isEnabled()).isFalse()).verifyComplete();
    }

    @Test
    void toggleEnabled_whenNotFound_shouldReturnEmpty() {
        when(r2dbcRepository.findById("non-existent")).thenReturn(Mono.empty());

        StepVerifier.create(repository.toggleEnabled("non-existent")).verifyComplete();
    }

    // ------------------------------------------------------------------
    // count
    // ------------------------------------------------------------------

    @Test
    void count_shouldDelegateToR2dbcRepository() {
        when(r2dbcRepository.count()).thenReturn(Mono.just(5L));

        StepVerifier.create(repository.count()).assertNext(c -> assertThat(c).isEqualTo(5L)).verifyComplete();

        verify(r2dbcRepository).count();
    }
}
