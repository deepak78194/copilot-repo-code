package com.example.analytics.infrastructure.persistence.repository;

import com.example.analytics.infrastructure.persistence.entity.AnalyticsRecordEntity;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

@JdbcRepository(dialect = Dialect.ORACLE, dataSource = "oracle")
public interface AnalyticsRecordJdbcRepository extends CrudRepository<AnalyticsRecordEntity, Long> {
}
