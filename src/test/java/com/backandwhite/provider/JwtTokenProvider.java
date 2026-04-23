package com.backandwhite.provider;

import com.backandwhite.common.constants.AppConstants;
import com.backandwhite.common.security.jwt.JwtProperties;
import com.backandwhite.common.security.jwt.JwtUtils;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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

    /**
     * Generates a structurally valid JWT that carries no {@code exp} claim, used to
     * verify that the filter does not reject tokens without expiry.
     */
    public static String tokenWithoutExp(String email) {
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = "{\"sub\":\"" + email + "\",\"email\":\"" + email + "\",\"roles\":[\"ROLE_CUSTOMER\"]}";
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".fakesignature";
    }

    /**
     * Generates a structurally valid JWT without any {@code roles} claim, used to
     * verify the admin-only path rejects tokens missing role information.
     */
    public static String tokenWithoutRoles(String email) {
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = "{\"sub\":\"" + email + "\",\"email\":\"" + email + "\"}";
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".fakesignature";
    }
}
