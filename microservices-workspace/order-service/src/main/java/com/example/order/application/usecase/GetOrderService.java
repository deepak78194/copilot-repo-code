package com.example.order.application.usecase;

import com.example.order.application.port.inbound.GetOrderUseCase;
import com.example.order.application.port.outbound.OrderRepositoryPort;
import com.example.order.domain.model.Order;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Optional;

@Singleton
public class GetOrderService implements GetOrderUseCase {

    private final OrderRepositoryPort orderRepositoryPort;

    public GetOrderService(OrderRepositoryPort orderRepositoryPort) {
        this.orderRepositoryPort = orderRepositoryPort;
    }

    @Override
    public Optional<Order> getOrderById(Long id) {
        return orderRepositoryPort.findById(id);
    }

    @Override
    public List<Order> getAllOrders() {
        return orderRepositoryPort.findAll();
    }
}
