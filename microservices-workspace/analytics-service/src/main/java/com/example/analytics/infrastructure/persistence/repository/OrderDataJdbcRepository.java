package com.example.analytics.infrastructure.persistence.repository;

import com.example.analytics.infrastructure.persistence.entity.OrderDataEntity;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.math.BigDecimal;
import java.util.List;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface OrderDataJdbcRepository extends CrudRepository<OrderDataEntity, Long> {

    @Query(value = "SELECT * FROM order_data ORDER BY order_created_at DESC", nativeQuery = true)
    List<OrderDataEntity> findAllOrderedByCreatedAt();

    @Query(value = "SELECT COALESCE(SUM(total_amount), 0) FROM order_data", nativeQuery = true)
    BigDecimal sumTotalAmount();
}
