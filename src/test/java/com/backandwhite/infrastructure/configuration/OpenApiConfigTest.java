package com.backandwhite.infrastructure.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.swagger.v3.oas.models.OpenAPI;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("OpenApiConfig")
class OpenApiConfigTest {

    private OpenApiConfig config;

    @BeforeEach
    void setUp() throws Exception {
        config = new OpenApiConfig();
        Field portField = OpenApiConfig.class.getDeclaredField("serverPort");
        portField.setAccessible(true);
        portField.set(config, 9000);
    }

    @Nested
    @DisplayName("objectMapper()")
    class ObjectMapperBean {

        @Test
        @DisplayName("returns non-null ObjectMapper with JavaTimeModule")
        void returnsConfiguredMapper() {
            ObjectMapper mapper = config.objectMapper();
            assertThat(mapper).isNotNull();
            assertThat(mapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)).isFalse();
        }
    }

    @Nested
    @DisplayName("customOpenAPI()")
    class CustomOpenApiBean {

        @Test
        @DisplayName("returns OpenAPI with correct title and version")
        void returnsOpenApiWithInfo() {
            OpenAPI api = config.customOpenAPI();
            assertThat(api).isNotNull();
            assertThat(api.getInfo().getTitle()).isEqualTo("API Gateway - Route Management");
            assertThat(api.getInfo().getVersion()).isEqualTo("1.0.0");
        }

        @Test
        @DisplayName("includes contact information")
        void hasContactInfo() {
            OpenAPI api = config.customOpenAPI();
            assertThat(api.getInfo().getContact().getName()).isEqualTo("NX036 Team");
            assertThat(api.getInfo().getContact().getEmail()).isEqualTo("dev@nx036.com");
        }

        @Test
        @DisplayName("includes local server with configured port")
        void hasServerUrl() {
            OpenAPI api = config.customOpenAPI();
            assertThat(api.getServers()).hasSize(1);
            assertThat(api.getServers().getFirst().getUrl()).isEqualTo("http://localhost:9000");
        }
    }
}
