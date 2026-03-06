package com.backandwhite.infrastructure.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.domain.ReactiveAuditorAware;
import reactor.core.publisher.Mono;

@Configuration
@EnableR2dbcAuditing(auditorAwareRef = "r2dbcAuditorAware")
public class R2dbcConfig {

    @Bean
    public ReactiveAuditorAware<String> r2dbcAuditorAware() {
        return () -> Mono.just("system");
    }
}
