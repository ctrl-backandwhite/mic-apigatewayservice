package com.backandwhite.provider;

import com.backandwhite.common.constants.AppConstants;
import com.backandwhite.common.security.jwt.JwtProperties;
import com.backandwhite.common.security.jwt.JwtUtils;
import java.util.List;

/**
 * Generates valid JWT tokens for tests.
 */
public final class JwtTokenProvider {

    public static final String TEST_SECRET = "test-secret-key-must-be-at-least-256-bits-long-for-hmac-sha256-algorithm";
    public static final String TEST_ISSUER = "test-issuer";
    private static final long EXPIRATION_MS = 3_600_000L;

    private JwtTokenProvider() {
    }

    public static String customerToken(String email) {
        return JwtUtils.generateToken(email, List.of(AppConstants.ROLE_CUSTOMER), TEST_SECRET, EXPIRATION_MS,
                TEST_ISSUER);
    }

    public static String adminToken(String email) {
        return JwtUtils.generateToken(email, List.of(AppConstants.ROLE_ADMIN), TEST_SECRET, EXPIRATION_MS, TEST_ISSUER);
    }

    public static String expiredToken(String email) {
        return JwtUtils.generateToken(email, List.of(AppConstants.ROLE_CUSTOMER), TEST_SECRET, -1000L, TEST_ISSUER);
    }

    public static String customerTokenWithIds(String email, Long customerId, Long employeeId) {
        JwtProperties props = new JwtProperties(TEST_SECRET, EXPIRATION_MS, TEST_ISSUER);
        return JwtUtils.generateToken(email, email, List.of(AppConstants.ROLE_CUSTOMER), customerId, employeeId, props);
    }
}
