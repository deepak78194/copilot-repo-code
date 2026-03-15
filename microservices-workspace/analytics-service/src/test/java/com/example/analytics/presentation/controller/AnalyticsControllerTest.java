package com.example.analytics.presentation.controller;

import com.example.analytics.application.port.inbound.GetAnalyticsUseCase;
import com.example.analytics.application.port.inbound.RecordAnalyticsUseCase;
import com.example.analytics.domain.model.AnalyticsRecord;
import com.example.analytics.domain.model.AnalyticsReport;
import com.example.analytics.domain.model.OrderData;
import com.example.analytics.presentation.controller.dto.AnalyticsRecordResponse;
import com.example.analytics.presentation.controller.dto.AnalyticsReportResponse;
import com.example.analytics.presentation.controller.dto.OrderDataResponse;
import com.example.analytics.presentation.controller.dto.RecordMetricRequest;
import io.micronaut.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    @Mock
    private GetAnalyticsUseCase getAnalyticsUseCase;

    @Mock
    private RecordAnalyticsUseCase recordAnalyticsUseCase;

    @InjectMocks
    private AnalyticsController analyticsController;

    @Test
    void shouldReturnAllOrderData() {
        List<OrderData> orderDataList = List.of(
                new OrderData(1L, 10L, 100L, "Laptop", 1, new BigDecimal("999.99"), "PENDING", LocalDateTime.now(), LocalDateTime.now())
        );
        when(getAnalyticsUseCase.getAllOrderData()).thenReturn(orderDataList);

        HttpResponse<List<OrderDataResponse>> response = analyticsController.getAllOrderData();

        assertThat(response.getStatus().getCode()).isEqualTo(200);
        assertThat(response.body()).hasSize(1);
        assertThat(response.body().get(0).productName()).isEqualTo("Laptop");
    }

    @Test
    void shouldGenerateReport() {
        AnalyticsReport report = new AnalyticsReport(10L, new BigDecimal("5000.00"), new BigDecimal("500.00"), 25L);
        when(getAnalyticsUseCase.generateReport()).thenReturn(report);

        HttpResponse<AnalyticsReportResponse> response = analyticsController.generateReport();

        assertThat(response.getStatus().getCode()).isEqualTo(200);
        assertThat(response.body()).isNotNull();
        assertThat(response.body().totalOrders()).isEqualTo(10L);
        assertThat(response.body().totalRevenue()).isEqualByComparingTo(new BigDecimal("5000.00"));
    }

    @Test
    void shouldRecordMetricAndReturn201() {
        AnalyticsRecord record = new AnalyticsRecord(1L, "page_views", new BigDecimal("150.75"), "homepage", LocalDateTime.now());
        when(recordAnalyticsUseCase.recordMetric("page_views", new BigDecimal("150.75"), "homepage"))
                .thenReturn(record);

        RecordMetricRequest request = new RecordMetricRequest("page_views", new BigDecimal("150.75"), "homepage");
        HttpResponse<AnalyticsRecordResponse> response = analyticsController.recordMetric(request, "req-456");

        assertThat(response.getStatus().getCode()).isEqualTo(201);
        assertThat(response.body()).isNotNull();
        assertThat(response.body().metricName()).isEqualTo("page_views");
    }
}
