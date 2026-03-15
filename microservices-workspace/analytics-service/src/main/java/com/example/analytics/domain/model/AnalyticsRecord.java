package com.example.analytics.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AnalyticsRecord {

    private Long id;
    private String metricName;
    private BigDecimal metricValue;
    private String dimension;
    private LocalDateTime recordedAt;

    public AnalyticsRecord() {
    }

    public AnalyticsRecord(Long id, String metricName, BigDecimal metricValue, String dimension, LocalDateTime recordedAt) {
        this.id = id;
        this.metricName = metricName;
        this.metricValue = metricValue;
        this.dimension = dimension;
        this.recordedAt = recordedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMetricName() { return metricName; }
    public void setMetricName(String metricName) { this.metricName = metricName; }
    public BigDecimal getMetricValue() { return metricValue; }
    public void setMetricValue(BigDecimal metricValue) { this.metricValue = metricValue; }
    public String getDimension() { return dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}
