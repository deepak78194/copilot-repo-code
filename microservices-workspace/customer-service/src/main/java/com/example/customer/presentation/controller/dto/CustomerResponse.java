package com.example.customer.presentation.controller.dto;

import com.example.customer.domain.model.Customer;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Serdeable
@Schema(description = "Customer response payload")
public record CustomerResponse(

        @Schema(description = "Customer unique identifier", example = "1")
        Long id,

        @Schema(description = "Customer first name", example = "John")
        String firstName,

        @Schema(description = "Customer last name", example = "Doe")
        String lastName,

        @Schema(description = "Customer email address", example = "john.doe@example.com")
        String email,

        @Schema(description = "Customer phone number", example = "+1-555-0123")
        String phone,

        @Schema(description = "Timestamp when the customer was created")
        LocalDateTime createdAt
) {

    public static CustomerResponse fromDomain(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getCreatedAt()
        );
    }
}
