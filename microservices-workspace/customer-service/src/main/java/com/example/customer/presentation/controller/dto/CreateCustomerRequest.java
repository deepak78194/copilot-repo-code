package com.example.customer.presentation.controller.dto;

import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Serdeable
@Schema(description = "Request payload for creating a new customer")
public record CreateCustomerRequest(

        @NotBlank
        @Size(max = 100)
        @Schema(description = "Customer first name", example = "John")
        String firstName,

        @NotBlank
        @Size(max = 100)
        @Schema(description = "Customer last name", example = "Doe")
        String lastName,

        @NotBlank
        @Email
        @Schema(description = "Customer email address", example = "john.doe@example.com")
        String email,

        @Size(max = 20)
        @Schema(description = "Customer phone number", example = "+1-555-0123")
        String phone
) {
}
