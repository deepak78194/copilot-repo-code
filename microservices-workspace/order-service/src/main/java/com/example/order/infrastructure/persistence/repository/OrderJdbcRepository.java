package com.example.order.infrastructure.persistence.repository;

import com.example.order.infrastructure.persistence.entity.OrderEntity;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface OrderJdbcRepository extends CrudRepository<OrderEntity, Long> {

    @Query(value = "SELECT * FROM orders WHERE id = :id", nativeQuery = true)
    Optional<OrderEntity> findOrderById(Long id);

    @Query(value = "SELECT * FROM orders ORDER BY created_at DESC", nativeQuery = true)
    List<OrderEntity> findAllOrders();
}
