package com.example.analytics;

import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(
        info = @Info(
                title = "Analytics Service API",
                version = "1.0.0",
                description = "Microservice for analytics with dual datasource (PostgreSQL read, Oracle write)",
                contact = @Contact(name = "Platform Team", email = "platform@example.com")
        )
)
public class Application {

    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }
}
