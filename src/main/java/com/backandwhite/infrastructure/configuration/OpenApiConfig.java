package com.backandwhite.infrastructure.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuración de OpenAPI/Swagger para el API Gateway.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private Integer serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Gateway - Route Management")
                        .version("1.0.0")
                        .description("API para gestión dinámica de rutas del API Gateway. " +
                                "Permite crear, actualizar, habilitar/deshabilitar y eliminar rutas en caliente.")
                        .contact(new Contact()
                                .name("Back&White Team")
                                .email("dev@backandwhite.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://backandwhite.com/license")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server")));
    }
}
