package io.github.ozkanpakdil.grepwise.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration class for OpenAPI documentation.
 * This class sets up the Swagger UI with information about the API.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Configures the OpenAPI documentation with information about the API.
     *
     * @return The OpenAPI configuration
     */
    @Bean
    public OpenAPI grepWiseOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("GrepWise API")
                        .description("GrepWise is an open-source alternative to Splunk, designed for log analysis and monitoring. " +
                                "This API provides endpoints for log search, analysis, dashboard management, and system configuration.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("GrepWise Team")
                                .url("https://github.com/ozkanpakdil/GrepWise")
                                .email("info@grepwise.org"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.grepwise.org")
                                .description("Production Server (Example)")));
    }
}