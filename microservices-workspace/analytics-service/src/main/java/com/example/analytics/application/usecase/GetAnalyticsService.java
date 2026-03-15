package com.example.analytics.application.usecase;

import com.example.analytics.application.port.inbound.GetAnalyticsUseCase;
import com.example.analytics.application.port.outbound.AnalyticsRecordPort;
import com.example.analytics.application.port.outbound.OrderDataPort;
import com.example.analytics.domain.model.AnalyticsReport;
import com.example.analytics.domain.model.OrderData;
import jakarta.inject.Singleton;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Singleton
public class GetAnalyticsService implements GetAnalyticsUseCase {

    private final OrderDataPort orderDataPort;
    private final AnalyticsRecordPort analyticsRecordPort;

    public GetAnalyticsService(OrderDataPort orderDataPort, AnalyticsRecordPort analyticsRecordPort) {
        this.orderDataPort = orderDataPort;
        this.analyticsRecordPort = analyticsRecordPort;
    }

    @Override
    public List<OrderData> getAllOrderData() {
        return orderDataPort.findAll();
    }

    @Override
    public AnalyticsReport generateReport() {
        long totalOrders = orderDataPort.count();
        BigDecimal totalRevenue = orderDataPort.sumTotalAmount();
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }
        BigDecimal averageOrderValue = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        long totalAnalyticsRecords = analyticsRecordPort.count();
        return new AnalyticsReport(totalOrders, totalRevenue, averageOrderValue, totalAnalyticsRecords);
    }
}
