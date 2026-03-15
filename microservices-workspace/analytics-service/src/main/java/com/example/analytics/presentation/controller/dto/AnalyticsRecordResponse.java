package com.example.analytics.presentation.controller.dto;

import com.example.analytics.domain.model.AnalyticsRecord;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Serdeable
@Schema(description = "Analytics record response")
public record AnalyticsRecordResponse(

        @Schema(description = "Record unique identifier")
        Long id,

        @Schema(description = "Metric name")
        String metricName,

        @Schema(description = "Metric value")
        BigDecimal metricValue,

        @Schema(description = "Dimension or category")
        String dimension,

        @Schema(description = "When the metric was recorded")
        LocalDateTime recordedAt
) {

    public static AnalyticsRecordResponse fromDomain(AnalyticsRecord record) {
        return new AnalyticsRecordResponse(
                record.getId(),
                record.getMetricName(),
                record.getMetricValue(),
                record.getDimension(),
                record.getRecordedAt()
        );
    }
}
