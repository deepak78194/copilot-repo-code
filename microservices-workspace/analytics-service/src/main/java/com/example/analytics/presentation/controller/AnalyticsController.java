package com.example.analytics.presentation.controller;

import com.example.analytics.application.port.inbound.GetAnalyticsUseCase;
import com.example.analytics.application.port.inbound.RecordAnalyticsUseCase;
import com.example.analytics.domain.model.AnalyticsRecord;
import com.example.analytics.domain.model.AnalyticsReport;
import com.example.analytics.presentation.controller.dto.AnalyticsRecordResponse;
import com.example.analytics.presentation.controller.dto.AnalyticsReportResponse;
import com.example.analytics.presentation.controller.dto.OrderDataResponse;
import com.example.analytics.presentation.controller.dto.RecordMetricRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import io.micronaut.validation.Validated;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.net.URI;
import java.util.List;

@Controller("/analytics")
@Validated
@Tag(name = "Analytics", description = "Analytics operations with dual datasource (PostgreSQL + Oracle)")
public class AnalyticsController {

    private final GetAnalyticsUseCase getAnalyticsUseCase;
    private final RecordAnalyticsUseCase recordAnalyticsUseCase;

    public AnalyticsController(GetAnalyticsUseCase getAnalyticsUseCase,
                               RecordAnalyticsUseCase recordAnalyticsUseCase) {
        this.getAnalyticsUseCase = getAnalyticsUseCase;
        this.recordAnalyticsUseCase = recordAnalyticsUseCase;
    }

    @Get("/orders")
    @Operation(summary = "Get all order data", description = "Retrieves all order data from the PostgreSQL analytics store")
    @ApiResponse(responseCode = "200", description = "List of all order data",
            content = @Content(schema = @Schema(implementation = OrderDataResponse.class)))
    public HttpResponse<List<OrderDataResponse>> getAllOrderData() {
        List<OrderDataResponse> responses = getAnalyticsUseCase.getAllOrderData().stream()
                .map(OrderDataResponse::fromDomain)
                .toList();
        return HttpResponse.ok(responses);
    }

    @Get("/report")
    @Operation(summary = "Generate analytics report", description = "Generates a summary analytics report combining data from both datasources")
    @ApiResponse(responseCode = "200", description = "Analytics summary report",
            content = @Content(schema = @Schema(implementation = AnalyticsReportResponse.class)))
    public HttpResponse<AnalyticsReportResponse> generateReport() {
        AnalyticsReport report = getAnalyticsUseCase.generateReport();
        return HttpResponse.ok(AnalyticsReportResponse.fromDomain(report));
    }

    @Post("/metrics")
    @Operation(summary = "Record an analytics metric", description = "Records a new analytics metric to the Oracle datasource")
    @ApiResponse(responseCode = "201", description = "Metric recorded successfully",
            content = @Content(schema = @Schema(implementation = AnalyticsRecordResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request payload")
    public HttpResponse<AnalyticsRecordResponse> recordMetric(
            @Body @Valid RecordMetricRequest request,
            @Header("X-Request-ID") @Parameter(description = "Request correlation ID") String requestId) {
        AnalyticsRecord record = recordAnalyticsUseCase.recordMetric(
                request.metricName(),
                request.metricValue(),
                request.dimension()
        );
        AnalyticsRecordResponse response = AnalyticsRecordResponse.fromDomain(record);
        return HttpResponse.created(response, URI.create("/analytics/metrics/" + record.getId()));
    }
}
