package org.springframework.boot.autoconfigure.web.servlet;

/**
 * Test stub for Spring Cloud 2025.0.0 / Spring Boot 4.0 compatibility.
 *
 * Spring Cloud Context 4.3.0 references WebMvcAutoConfiguration
 * via @AutoConfigureAfter in LifecycleMvcEndpointAutoConfiguration and
 * RefreshAutoConfiguration. In Spring Boot 4.0, WebMvcAutoConfiguration was
 * removed (MVC auto-config split into a separate module). This stub allows
 * class loading to succeed so Spring Cloud conditions can evaluate properly.
 * The class itself is never instantiated
 * because @ConditionalOnWebApplication(type = SERVLET) prevents it from being
 * used in a reactive (WebFlux) context.
 */
public class WebMvcAutoConfiguration {
}
