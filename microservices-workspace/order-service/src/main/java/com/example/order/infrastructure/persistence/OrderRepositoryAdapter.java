package com.example.order.infrastructure.persistence;

import com.example.order.application.port.outbound.OrderRepositoryPort;
import com.example.order.domain.model.Order;
import com.example.order.infrastructure.persistence.entity.OrderEntity;
import com.example.order.infrastructure.persistence.repository.OrderJdbcRepository;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Optional;

@Singleton
public class OrderRepositoryAdapter implements OrderRepositoryPort {

    private final OrderJdbcRepository orderJdbcRepository;

    public OrderRepositoryAdapter(OrderJdbcRepository orderJdbcRepository) {
        this.orderJdbcRepository = orderJdbcRepository;
    }

    @Override
    public Order save(Order order) {
        OrderEntity entity = toEntity(order);
        OrderEntity saved = orderJdbcRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Order> findById(Long id) {
        return orderJdbcRepository.findOrderById(id).map(this::toDomain);
    }

    @Override
    public List<Order> findAll() {
        return orderJdbcRepository.findAllOrders().stream()
                .map(this::toDomain)
                .toList();
    }

    private OrderEntity toEntity(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.setId(order.getId());
        entity.setCustomerId(order.getCustomerId());
        entity.setProductName(order.getProductName());
        entity.setQuantity(order.getQuantity());
        entity.setTotalAmount(order.getTotalAmount());
        entity.setStatus(order.getStatus());
        entity.setCreatedAt(order.getCreatedAt());
        return entity;
    }

    private Order toDomain(OrderEntity entity) {
        return new Order(
                entity.getId(),
                entity.getCustomerId(),
                entity.getProductName(),
                entity.getQuantity(),
                entity.getTotalAmount(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
