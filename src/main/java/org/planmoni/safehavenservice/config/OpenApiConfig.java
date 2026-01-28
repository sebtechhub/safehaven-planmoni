package org.planmoni.safehavenservice.config;

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
 * OpenAPI 3.1 (Swagger) Configuration for SafeHaven Service API
 * Using springdoc-openapi v3.x for Spring Boot 4.0+ compatibility
 * 
 * Access Swagger UI at: http://localhost:8080/swagger-ui/index.html
 * Access OpenAPI JSON at: http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:safehaven-service}")
    private String applicationName;

    @Bean
    public OpenAPI safeHavenOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SafeHaven Service API")
                        .description("""
                                RESTful API for managing SafeHaven accounts in the Planmoni ecosystem.
                                
                                This service provides endpoints for:
                                - Creating and managing SafeHaven accounts
                                - Querying account details by ID or reference
                                - Updating account information
                                - Suspending accounts
                                - Retrieving paginated account lists
                                
                                All endpoints follow REST best practices and return consistent error responses.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Planmoni Engineering Team")
                                .email("engineering@planmoni.org")
                                .url("https://planmoni.org"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://planmoni.org/license")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.planmoni.org")
                                .description("Production Server")
                ));
    }
}
