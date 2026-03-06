package com.backandwhite;

import com.backandwhite.provider.JwtTokenProvider;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Testcontainers;

@Log4j2
@Testcontainers
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@Import({ TestcontainersConfiguration.class, TestSecurityConfig.class })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@TestPropertySource(properties = "spring.liquibase.enabled=true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class BaseIntegration {

    @LocalServerPort
    private int port;

    protected WebTestClient webTestClient;

    static {
        org.testcontainers.utility.TestcontainersConfiguration.getInstance()
                .updateUserConfig("checks.disable", "true");
    }

    @BeforeEach
    public void setUpBase() {
        if (webTestClient == null) {
            webTestClient = WebTestClient.bindToServer()
                    .baseUrl("http://localhost:" + port)
                    .build();
        }
    }

    /**
     * Creates a WebTestClient with an admin JWT token pre-configured.
     */
    protected WebTestClient adminClient() {
        String token = JwtTokenProvider.adminToken("admin@test.com");
        return WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
    }

    /**
     * Creates a WebTestClient with a customer JWT token pre-configured.
     */
    protected WebTestClient customerClient() {
        String token = JwtTokenProvider.customerToken("customer@test.com");
        return WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
    }
}
