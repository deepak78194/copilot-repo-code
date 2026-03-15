package com.example.analytics.application.port.inbound;

import com.example.analytics.domain.model.AnalyticsRecord;

public interface RecordAnalyticsUseCase {

    AnalyticsRecord recordMetric(String metricName, java.math.BigDecimal metricValue, String dimension);
}
