package org.springframework.boot.web.context;

import org.springframework.context.ApplicationEvent;

/**
 * Test stub — Spring Cloud Commons 4.3.0 / Spring Boot 4.0 compatibility shim.
 *
 * In Spring Boot 4.0, WebServerInitializedEvent was moved to
 * org.springframework.boot.web.server.context. Spring Cloud Commons 4.3.0 still
 * references the old package path via SimpleDiscoveryClientAutoConfiguration and
 * SimpleReactiveDiscoveryClientAutoConfiguration. This stub allows class loading to
 * succeed so Spring Cloud conditions can evaluate properly.
 */
public abstract class WebServerInitializedEvent extends ApplicationEvent {

    protected WebServerInitializedEvent(Object source) {
        super(source);
    }
}
