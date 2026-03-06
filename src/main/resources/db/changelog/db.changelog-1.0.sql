--liquibase formatted sql

--changeset Jesus.Finol:1
CREATE TABLE IF NOT EXISTS gateway_route (
    id          VARCHAR(255)                NOT NULL,
    uri         VARCHAR(1000)               NOT NULL,
    predicates  TEXT                        NOT NULL DEFAULT '[]',
    filters     TEXT                        NOT NULL DEFAULT '[]',
    route_order INTEGER                     NOT NULL DEFAULT 0,
    enabled     BOOLEAN                     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_gateway_route PRIMARY KEY (id)
);

COMMENT ON TABLE gateway_route IS 'Rutas dinámicas del API Gateway. Modificables en caliente sin redeploy.';
COMMENT ON COLUMN gateway_route.predicates IS 'JSON array de predicates en formato shortcut: ["Path=/api/v1/**"]';
COMMENT ON COLUMN gateway_route.filters    IS 'JSON array de filters en formato shortcut: ["RequestRateLimiter=10,20"]';
