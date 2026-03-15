package com.example.order.application.usecase;

import com.example.order.application.port.inbound.CreateOrderUseCase;
import com.example.order.application.port.outbound.OrderRepositoryPort;
import com.example.order.domain.model.Order;
import jakarta.inject.Singleton;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Singleton
public class CreateOrderService implements CreateOrderUseCase {

    private final OrderRepositoryPort orderRepositoryPort;

    public CreateOrderService(OrderRepositoryPort orderRepositoryPort) {
        this.orderRepositoryPort = orderRepositoryPort;
    }

    @Override
    public Order createOrder(String productName, Long customerId, Integer quantity, BigDecimal totalAmount) {
        Order order = new Order();
        order.setCustomerId(customerId);
        order.setProductName(productName);
        order.setQuantity(quantity);
        order.setTotalAmount(totalAmount);
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());
        return orderRepositoryPort.save(order);
    }
}
