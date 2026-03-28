--liquibase formatted sql

--changeset Jesus.Finol:3
--comment: Add ecommerce frontend catch-all route. Routes all unmatched traffic to the nx036 ecommerce SPA.
INSERT INTO gateway_route (id, uri, predicates, filters, route_order, enabled)
VALUES (
    'ecomerce-frontend',
    'http://nx036-ecomerce.railway.internal',
    '["Path=/**"]',
    '[]',
    100,
    TRUE
);

COMMENT ON TABLE gateway_route IS 'Rutas dinámicas del API Gateway. Modificables en caliente sin redeploy.';

--rollback DELETE FROM gateway_route WHERE id = 'ecomerce-frontend';
