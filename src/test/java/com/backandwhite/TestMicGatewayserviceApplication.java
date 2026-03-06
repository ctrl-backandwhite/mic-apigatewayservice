package com.backandwhite;

import org.springframework.boot.SpringApplication;

public class TestMicGatewayserviceApplication {

    public static void main(String[] args) {
        SpringApplication.from(MicGatewayserviceApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
