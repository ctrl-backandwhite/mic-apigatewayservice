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
