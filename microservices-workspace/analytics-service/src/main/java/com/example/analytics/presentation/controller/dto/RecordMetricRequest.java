package com.example.analytics.presentation.controller.dto;

import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Serdeable
@Schema(description = "Request payload for recording an analytics metric")
public record RecordMetricRequest(

        @NotBlank
        @Schema(description = "Name of the metric", example = "page_views")
        String metricName,

        @NotNull
        @DecimalMin("0.0")
        @Schema(description = "Value of the metric", example = "150.75")
        BigDecimal metricValue,

        @Schema(description = "Dimension or category for the metric", example = "homepage")
        String dimension
) {
}
