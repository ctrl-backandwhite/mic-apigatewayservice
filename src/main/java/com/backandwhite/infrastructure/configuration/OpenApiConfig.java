package com.backandwhite.infrastructure.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuración de OpenAPI/Swagger para el API Gateway.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private Integer serverPort;

    @Primary
    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("API Gateway - Route Management").version("1.0.0")
                        .description("API para gestión dinámica de rutas del API Gateway. "
                                + "Permite crear, actualizar, habilitar/deshabilitar y eliminar rutas en caliente.")
                        .contact(new Contact().name("NX036 Team").email("dev@nx036.com"))
                        .license(new License().name("Proprietary").url("https://nx036.com/license")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Local Development Server")));
    }
}
