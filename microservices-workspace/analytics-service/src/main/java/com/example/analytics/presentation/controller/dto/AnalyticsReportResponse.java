package com.example.analytics.presentation.controller.dto;

import com.example.analytics.domain.model.AnalyticsReport;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Serdeable
@Schema(description = "Analytics summary report")
public record AnalyticsReportResponse(

        @Schema(description = "Total number of orders")
        long totalOrders,

        @Schema(description = "Total revenue across all orders")
        BigDecimal totalRevenue,

        @Schema(description = "Average order value")
        BigDecimal averageOrderValue,

        @Schema(description = "Total number of analytics records stored")
        long totalAnalyticsRecords
) {

    public static AnalyticsReportResponse fromDomain(AnalyticsReport report) {
        return new AnalyticsReportResponse(
                report.getTotalOrders(),
                report.getTotalRevenue(),
                report.getAverageOrderValue(),
                report.getTotalAnalyticsRecords()
        );
    }
}
