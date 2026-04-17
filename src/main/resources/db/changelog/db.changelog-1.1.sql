ALTER TABLE gateway_route
    ADD COLUMN rate_limit_replenish_rate  INTEGER,
    ADD COLUMN rate_limit_burst_capacity  INTEGER,
    ADD COLUMN rate_limit_requested_tokens INTEGER DEFAULT 1;

COMMENT ON COLUMN gateway_route.rate_limit_replenish_rate  IS 'Tokens added per second (sustained rate). NULL = no rate limit on this route.';
COMMENT ON COLUMN gateway_route.rate_limit_burst_capacity  IS 'Maximum bucket capacity (peak burst). NULL = no rate limit on this route.';
COMMENT ON COLUMN gateway_route.rate_limit_requested_tokens IS 'Tokens consumed per request. Defaults to 1.';
