package com.project.agent.domain.bootstrap.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Spring configuration that defines the OpenAPI/Swagger metadata (title, description,
 * version, server) for the Agent Service REST API.
 */
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI agentOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Agent Service API")
                        .description("REST API for Agent service in the SAAS platform")
                        .version("1.0.0")
                )
                .servers(List.of(
                        new Server().url("http://localhost:8083").description("Local server")
                ));
    }
}
