package org.springframework.boot.autoconfigure.web.embedded;

/**
 * Test stub — Spring Cloud Gateway 4.3.0 / Spring Boot 4.0 compatibility shim.
 *
 * GatewayAutoConfiguration$NettyConfiguration references NettyWebServerFactoryCustomizer
 * via @ConditionalOnClass. In Spring Boot 4.0, this class was removed from
 * spring-boot-autoconfigure (Netty configuration moved to spring-boot-netty module
 * with a different package structure). This stub allows class introspection to succeed
 * so Spring Cloud Gateway conditions can evaluate properly.
 */
public class NettyWebServerFactoryCustomizer {
}
