package com.workflow.ForgeFlow.workflow.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI forgeFlowOpenApi() {
        return new OpenAPI().info(new Info()
                .title("ForgeFlow")
                .version("v1")
                .description("Enqueue workflow jobs, poll status, and generate a job from a prompt."));
    }
}
