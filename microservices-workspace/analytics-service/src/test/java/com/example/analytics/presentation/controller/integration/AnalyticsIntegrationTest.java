package com.example.analytics.presentation.controller.integration;

import com.example.analytics.application.port.outbound.AnalyticsRecordPort;
import com.example.analytics.application.port.outbound.OrderDataPort;
import com.example.analytics.domain.model.AnalyticsRecord;
import com.example.analytics.domain.model.OrderData;
import com.example.analytics.presentation.controller.dto.AnalyticsRecordResponse;
import com.example.analytics.presentation.controller.dto.AnalyticsReportResponse;
import com.example.analytics.presentation.controller.dto.OrderDataResponse;
import com.example.analytics.presentation.controller.dto.RecordMetricRequest;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest
class AnalyticsIntegrationTest {

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Inject
    OrderDataPort orderDataPort;

    @Inject
    AnalyticsRecordPort analyticsRecordPort;

    @MockBean(OrderDataPort.class)
    OrderDataPort mockOrderDataPort() {
        return mock(OrderDataPort.class);
    }

    @MockBean(AnalyticsRecordPort.class)
    AnalyticsRecordPort mockAnalyticsRecordPort() {
        return mock(AnalyticsRecordPort.class);
    }

    @Test
    void shouldGetAllOrderDataViaHttp() {
        List<OrderData> orderDataList = List.of(
                new OrderData(1L, 10L, 100L, "Laptop", 1, new BigDecimal("999.99"), "PENDING", LocalDateTime.now(), LocalDateTime.now())
        );
        when(orderDataPort.findAll()).thenReturn(orderDataList);

        HttpResponse<List<OrderDataResponse>> response = httpClient.toBlocking().exchange(
                HttpRequest.GET("/analytics/orders"),
                Argument.listOf(OrderDataResponse.class)
        );

        assertThat((Object) response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.body()).hasSize(1);
        assertThat(response.body().get(0).productName()).isEqualTo("Laptop");
    }

    @Test
    void shouldGenerateReportViaHttp() {
        when(orderDataPort.count()).thenReturn(5L);
        when(orderDataPort.sumTotalAmount()).thenReturn(new BigDecimal("2500.00"));
        when(analyticsRecordPort.count()).thenReturn(10L);

        HttpResponse<AnalyticsReportResponse> response = httpClient.toBlocking().exchange(
                HttpRequest.GET("/analytics/report"),
                AnalyticsReportResponse.class
        );

        assertThat((Object) response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.body()).isNotNull();
        assertThat(response.body().totalOrders()).isEqualTo(5L);
        assertThat(response.body().totalRevenue()).isEqualByComparingTo(new BigDecimal("2500.00"));
        assertThat(response.body().averageOrderValue()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void shouldRecordMetricViaHttp() {
        AnalyticsRecord saved = new AnalyticsRecord(1L, "page_views", new BigDecimal("150.75"), "homepage", LocalDateTime.now());
        when(analyticsRecordPort.save(any(AnalyticsRecord.class))).thenReturn(saved);

        RecordMetricRequest request = new RecordMetricRequest("page_views", new BigDecimal("150.75"), "homepage");
        HttpResponse<AnalyticsRecordResponse> response = httpClient.toBlocking().exchange(
                HttpRequest.POST("/analytics/metrics", request).header("X-Request-ID", "test-789"),
                AnalyticsRecordResponse.class
        );

        assertThat((Object) response.getStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.body()).isNotNull();
        assertThat(response.body().metricName()).isEqualTo("page_views");
    }
}
