package com.backandwhite.infrastructure.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * R2DBC entity representing a gateway route in PostgreSQL. Predicates and
 * filters are serialized as JSON (TEXT in the database).
 */
@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("gateway_route")
public class GatewayRouteEntity {

    @Id
    private String id;

    private String uri;

    /** Predicates serialized as JSON array: ["Path=/api/v1/**"] */
    private String predicates;

    /** Filters serialized as JSON array: ["RequestRateLimiter=10,20"] */
    private String filters;

    @Column("route_order")
    private int order;

    private boolean enabled;

    @Column("rate_limit_replenish_rate")
    private Integer rateLimitReplenishRate;

    @Column("rate_limit_burst_capacity")
    private Integer rateLimitBurstCapacity;

    @Column("rate_limit_requested_tokens")
    private Integer rateLimitRequestedTokens;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
