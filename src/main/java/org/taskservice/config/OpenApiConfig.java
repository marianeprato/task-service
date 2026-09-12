package org.taskservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI taskServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Task Service API")
                .description("Manages tasks and, on creation, publishes a TaskCreated event " +
                        "consumed by reminder-service. Also exposes a resilient read path to " +
                        "fetch reminders for a task.")
                .version("v1"));
    }
}
