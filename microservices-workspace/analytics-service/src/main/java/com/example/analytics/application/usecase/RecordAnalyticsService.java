package com.example.analytics.application.usecase;

import com.example.analytics.application.port.inbound.RecordAnalyticsUseCase;
import com.example.analytics.application.port.outbound.AnalyticsRecordPort;
import com.example.analytics.domain.model.AnalyticsRecord;
import jakarta.inject.Singleton;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Singleton
public class RecordAnalyticsService implements RecordAnalyticsUseCase {

    private final AnalyticsRecordPort analyticsRecordPort;

    public RecordAnalyticsService(AnalyticsRecordPort analyticsRecordPort) {
        this.analyticsRecordPort = analyticsRecordPort;
    }

    @Override
    public AnalyticsRecord recordMetric(String metricName, BigDecimal metricValue, String dimension) {
        AnalyticsRecord record = new AnalyticsRecord();
        record.setMetricName(metricName);
        record.setMetricValue(metricValue);
        record.setDimension(dimension);
        record.setRecordedAt(LocalDateTime.now());
        return analyticsRecordPort.save(record);
    }
}
