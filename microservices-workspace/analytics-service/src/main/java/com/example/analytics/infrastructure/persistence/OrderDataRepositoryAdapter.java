package com.example.analytics.infrastructure.persistence;

import com.example.analytics.application.port.outbound.OrderDataPort;
import com.example.analytics.domain.model.OrderData;
import com.example.analytics.infrastructure.persistence.entity.OrderDataEntity;
import com.example.analytics.infrastructure.persistence.repository.OrderDataJdbcRepository;
import jakarta.inject.Singleton;

import java.math.BigDecimal;
import java.util.List;

@Singleton
public class OrderDataRepositoryAdapter implements OrderDataPort {

    private final OrderDataJdbcRepository orderDataJdbcRepository;

    public OrderDataRepositoryAdapter(OrderDataJdbcRepository orderDataJdbcRepository) {
        this.orderDataJdbcRepository = orderDataJdbcRepository;
    }

    @Override
    public List<OrderData> findAll() {
        return orderDataJdbcRepository.findAllOrderedByCreatedAt().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long count() {
        return orderDataJdbcRepository.count();
    }

    @Override
    public BigDecimal sumTotalAmount() {
        return orderDataJdbcRepository.sumTotalAmount();
    }

    private OrderData toDomain(OrderDataEntity entity) {
        return new OrderData(
                entity.getId(),
                entity.getOrderId(),
                entity.getCustomerId(),
                entity.getProductName(),
                entity.getQuantity(),
                entity.getTotalAmount(),
                entity.getStatus(),
                entity.getOrderCreatedAt(),
                entity.getSyncedAt()
        );
    }
}
