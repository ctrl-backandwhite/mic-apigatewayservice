package com.backandwhite;

import com.backandwhite.common.configuration.annotation.EnableCoreApplication;
import com.backandwhite.common.security.jwt.JwtProperties;
import com.backandwhite.infrastructure.configuration.ServicesProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableCoreApplication
@EnableConfigurationProperties({JwtProperties.class, ServicesProperties.class})
public class MicGatewayserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MicGatewayserviceApplication.class, args);
    }
}
