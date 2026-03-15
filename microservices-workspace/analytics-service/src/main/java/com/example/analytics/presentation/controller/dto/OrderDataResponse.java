package com.example.analytics.presentation.controller.dto;

import com.example.analytics.domain.model.OrderData;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Serdeable
@Schema(description = "Order data response for analytics")
public record OrderDataResponse(

        @Schema(description = "Internal analytics ID")
        Long id,

        @Schema(description = "Original order ID")
        Long orderId,

        @Schema(description = "Customer ID who placed the order")
        Long customerId,

        @Schema(description = "Product name")
        String productName,

        @Schema(description = "Quantity ordered")
        Integer quantity,

        @Schema(description = "Total order amount")
        BigDecimal totalAmount,

        @Schema(description = "Order status")
        String status,

        @Schema(description = "When the order was originally created")
        LocalDateTime orderCreatedAt,

        @Schema(description = "When the order data was synced to analytics")
        LocalDateTime syncedAt
) {

    public static OrderDataResponse fromDomain(OrderData orderData) {
        return new OrderDataResponse(
                orderData.getId(),
                orderData.getOrderId(),
                orderData.getCustomerId(),
                orderData.getProductName(),
                orderData.getQuantity(),
                orderData.getTotalAmount(),
                orderData.getStatus(),
                orderData.getOrderCreatedAt(),
                orderData.getSyncedAt()
        );
    }
}
