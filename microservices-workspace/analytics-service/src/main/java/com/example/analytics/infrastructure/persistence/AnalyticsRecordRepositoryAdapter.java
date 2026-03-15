package com.example.analytics.infrastructure.persistence;

import com.example.analytics.application.port.outbound.AnalyticsRecordPort;
import com.example.analytics.domain.model.AnalyticsRecord;
import com.example.analytics.infrastructure.persistence.entity.AnalyticsRecordEntity;
import com.example.analytics.infrastructure.persistence.repository.AnalyticsRecordJdbcRepository;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class AnalyticsRecordRepositoryAdapter implements AnalyticsRecordPort {

    private final AnalyticsRecordJdbcRepository analyticsRecordJdbcRepository;

    public AnalyticsRecordRepositoryAdapter(AnalyticsRecordJdbcRepository analyticsRecordJdbcRepository) {
        this.analyticsRecordJdbcRepository = analyticsRecordJdbcRepository;
    }

    @Override
    public AnalyticsRecord save(AnalyticsRecord record) {
        AnalyticsRecordEntity entity = toEntity(record);
        AnalyticsRecordEntity saved = analyticsRecordJdbcRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<AnalyticsRecord> findAll() {
        List<AnalyticsRecord> records = new ArrayList<>();
        analyticsRecordJdbcRepository.findAll().forEach(entity -> records.add(toDomain(entity)));
        return records;
    }

    @Override
    public long count() {
        return analyticsRecordJdbcRepository.count();
    }

    private AnalyticsRecordEntity toEntity(AnalyticsRecord record) {
        AnalyticsRecordEntity entity = new AnalyticsRecordEntity();
        entity.setId(record.getId());
        entity.setMetricName(record.getMetricName());
        entity.setMetricValue(record.getMetricValue());
        entity.setDimension(record.getDimension());
        entity.setRecordedAt(record.getRecordedAt());
        return entity;
    }

    private AnalyticsRecord toDomain(AnalyticsRecordEntity entity) {
        return new AnalyticsRecord(
                entity.getId(),
                entity.getMetricName(),
                entity.getMetricValue(),
                entity.getDimension(),
                entity.getRecordedAt()
        );
    }
}
