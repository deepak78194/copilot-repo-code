package com.example.analytics.application.usecase;

import com.example.analytics.application.port.outbound.AnalyticsRecordPort;
import com.example.analytics.application.port.outbound.OrderDataPort;
import com.example.analytics.domain.model.AnalyticsReport;
import com.example.analytics.domain.model.OrderData;
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
class GetAnalyticsServiceTest {

    @Mock
    private OrderDataPort orderDataPort;

    @Mock
    private AnalyticsRecordPort analyticsRecordPort;

    @InjectMocks
    private GetAnalyticsService getAnalyticsService;

    @Test
    void shouldReturnAllOrderData() {
        List<OrderData> orderDataList = List.of(
                new OrderData(1L, 10L, 100L, "Laptop", 1, new BigDecimal("999.99"), "PENDING", LocalDateTime.now(), LocalDateTime.now()),
                new OrderData(2L, 11L, 101L, "Mouse", 3, new BigDecimal("29.97"), "COMPLETED", LocalDateTime.now(), LocalDateTime.now())
        );
        when(orderDataPort.findAll()).thenReturn(orderDataList);

        List<OrderData> result = getAnalyticsService.getAllOrderData();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getProductName()).isEqualTo("Laptop");
    }

    @Test
    void shouldGenerateReportWithCorrectCalculations() {
        when(orderDataPort.count()).thenReturn(2L);
        when(orderDataPort.sumTotalAmount()).thenReturn(new BigDecimal("1029.96"));
        when(analyticsRecordPort.count()).thenReturn(5L);

        AnalyticsReport report = getAnalyticsService.generateReport();

        assertThat(report.getTotalOrders()).isEqualTo(2L);
        assertThat(report.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("1029.96"));
        assertThat(report.getAverageOrderValue()).isEqualByComparingTo(new BigDecimal("514.98"));
        assertThat(report.getTotalAnalyticsRecords()).isEqualTo(5L);
    }

    @Test
    void shouldHandleZeroOrdersInReport() {
        when(orderDataPort.count()).thenReturn(0L);
        when(orderDataPort.sumTotalAmount()).thenReturn(null);
        when(analyticsRecordPort.count()).thenReturn(0L);

        AnalyticsReport report = getAnalyticsService.generateReport();

        assertThat(report.getTotalOrders()).isEqualTo(0L);
        assertThat(report.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(report.getAverageOrderValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
