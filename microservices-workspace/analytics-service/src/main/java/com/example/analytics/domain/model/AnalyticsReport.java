package com.example.analytics.domain.model;

import java.math.BigDecimal;

public class AnalyticsReport {

    private final long totalOrders;
    private final BigDecimal totalRevenue;
    private final BigDecimal averageOrderValue;
    private final long totalAnalyticsRecords;

    public AnalyticsReport(long totalOrders, BigDecimal totalRevenue, BigDecimal averageOrderValue, long totalAnalyticsRecords) {
        this.totalOrders = totalOrders;
        this.totalRevenue = totalRevenue;
        this.averageOrderValue = averageOrderValue;
        this.totalAnalyticsRecords = totalAnalyticsRecords;
    }

    public long getTotalOrders() { return totalOrders; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public BigDecimal getAverageOrderValue() { return averageOrderValue; }
    public long getTotalAnalyticsRecords() { return totalAnalyticsRecords; }
}
