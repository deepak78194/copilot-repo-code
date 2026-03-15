package com.example.order.application.port.inbound;

import com.example.order.domain.model.Order;

import java.math.BigDecimal;

public interface CreateOrderUseCase {

    Order createOrder(String productName, Long customerId, Integer quantity, BigDecimal totalAmount);
}
