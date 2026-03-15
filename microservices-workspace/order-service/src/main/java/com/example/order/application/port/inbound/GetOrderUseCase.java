package com.example.order.application.port.inbound;

import com.example.order.domain.model.Order;

import java.util.List;
import java.util.Optional;

public interface GetOrderUseCase {

    Optional<Order> getOrderById(Long id);

    List<Order> getAllOrders();
}
