package com.backandwhite.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * URLs of the ecosystem's microservices. Injected from
 * application-{profile}.yml. Used by
 * {@link com.backandwhite.infrastructure.seeder.RouteSeeder} to seed the
 * initial routes in PostgreSQL.
 *
 * <p>
 * Only services used by the seeder are declared here. If a new service is added
 * to the seeder, add the field here and its URL in the configuration profiles.
 */
@ConfigurationProperties(prefix = "services")
public record ServicesProperties(Service auth, Service notification, Service catalog, Service cms, Service orders,
        Service payments, Service webapp, Service ecommerce) {
    public record Service(String url) {
    }
}
