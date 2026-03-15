package com.example.order.presentation.controller.dto;

import com.example.order.domain.model.Order;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Serdeable
@Schema(description = "Response payload representing an order")
public record OrderResponse(

        @Schema(description = "Unique order identifier", example = "1")
        Long id,

        @Schema(description = "Customer ID who placed the order", example = "1001")
        Long customerId,

        @Schema(description = "Name of the ordered product", example = "Wireless Keyboard")
        String productName,

        @Schema(description = "Quantity ordered", example = "2")
        Integer quantity,

        @Schema(description = "Total order amount", example = "59.99")
        BigDecimal totalAmount,

        @Schema(description = "Current order status", example = "PENDING")
        String status,

        @Schema(description = "Timestamp when the order was created")
        LocalDateTime createdAt
) {

    public static OrderResponse fromDomain(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getProductName(),
                order.getQuantity(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }
}
