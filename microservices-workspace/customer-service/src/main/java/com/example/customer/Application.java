package com.example.customer;

import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(
        info = @Info(
                title = "Customer Service API",
                version = "1.0.0",
                description = "Microservice for managing customers using Oracle stored procedures",
                contact = @Contact(name = "Platform Team", email = "platform@example.com")
        )
)
public class Application {

    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }
}
