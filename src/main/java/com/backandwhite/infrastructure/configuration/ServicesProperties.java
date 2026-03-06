package com.backandwhite.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * URLs de los microservicios del ecosistema.
 * Inyectadas desde application-{profile}.yml.
 * Usadas por el {@link com.backandwhite.infrastructure.seeder.RouteSeeder}
 * para sembrar las rutas iniciales en PostgreSQL.
 */
@ConfigurationProperties(prefix = "services")
public record ServicesProperties(
        Service config,
        Service iam,
        Service catalog,
        Service customer,
        Service tax,
        Service inventory,
        Service pricing,
        Service shipping,
        Service cart,
        Service order,
        Service payment,
        Service notification,
        Service cms,
        Service media,
        Service analytics
) {
    public record Service(String url) {}
}
