package com.backandwhite;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

@TestConfiguration
@EnableWebFluxSecurity
public class TestSecurityConfig {

    @Bean
    @Primary
    public SecurityWebFilterChain testSecurityFilterChain(ServerHttpSecurity http) {
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable).httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll()).build();
    }

    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just("test-key");
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
