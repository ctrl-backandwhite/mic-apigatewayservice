package com.backandwhite.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * URLs de los microservicios del ecosistema. Inyectadas desde
 * application-{profile}.yml. Usadas por el
 * {@link com.backandwhite.infrastructure.seeder.RouteSeeder} para sembrar las
 * rutas iniciales en PostgreSQL.
 *
 * <p>
 * Solo se declaran los servicios que se usan en el seeder. Si se agrega un
 * nuevo servicio al seeder, añadir el campo aquí y su URL en los perfiles de
 * configuración.
 */
@ConfigurationProperties(prefix = "services")
public record ServicesProperties(Service auth, Service notification, Service catalog, Service cms, Service orders,
        Service payments, Service webapp, Service ecommerce) {
    public record Service(String url) {
    }
}
