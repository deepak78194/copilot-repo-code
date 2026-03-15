package com.example.order.presentation.controller.dto;

import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Serdeable
@Schema(description = "Request payload for creating a new order")
public record CreateOrderRequest(

        @NotNull
        @Schema(description = "ID of the customer placing the order", example = "1001")
        Long customerId,

        @NotBlank
        @Schema(description = "Name of the product being ordered", example = "Wireless Keyboard")
        String productName,

        @NotNull
        @Min(1)
        @Schema(description = "Quantity of items ordered", example = "2")
        Integer quantity,

        @NotNull
        @DecimalMin("0.01")
        @Schema(description = "Total monetary amount of the order", example = "59.99")
        BigDecimal totalAmount
) {
}
