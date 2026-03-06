package com.backandwhite;

import com.backandwhite.common.configuration.annotation.EnableCoreApplication;
import com.backandwhite.common.security.jwt.JwtProperties;
import com.backandwhite.infrastructure.configuration.ServicesProperties;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableCoreApplication
@OpenAPIDefinition(servers = {
        @Server(url = "https://mic-apigatewayservice-production.up.railway.app", description = "Production Server."),
        @Server(url = "https://localhost:3030", description = "Local Server.")
})
@EnableConfigurationProperties({JwtProperties.class, ServicesProperties.class})
public class MicGatewayserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MicGatewayserviceApplication.class, args);
    }
}
