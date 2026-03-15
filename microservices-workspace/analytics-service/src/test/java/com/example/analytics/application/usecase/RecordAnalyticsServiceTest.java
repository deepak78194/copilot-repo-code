package com.example.analytics.application.usecase;

import com.example.analytics.application.port.outbound.AnalyticsRecordPort;
import com.example.analytics.domain.model.AnalyticsRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordAnalyticsServiceTest {

    @Mock
    private AnalyticsRecordPort analyticsRecordPort;

    @InjectMocks
    private RecordAnalyticsService recordAnalyticsService;

    @Test
    void shouldRecordMetricWithCorrectFields() {
        when(analyticsRecordPort.save(any(AnalyticsRecord.class))).thenAnswer(invocation -> {
            AnalyticsRecord r = invocation.getArgument(0);
            r.setId(1L);
            return r;
        });

        AnalyticsRecord result = recordAnalyticsService.recordMetric("page_views", new BigDecimal("150.75"), "homepage");

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getMetricName()).isEqualTo("page_views");
        assertThat(result.getMetricValue()).isEqualByComparingTo(new BigDecimal("150.75"));
        assertThat(result.getDimension()).isEqualTo("homepage");
        assertThat(result.getRecordedAt()).isNotNull();

        ArgumentCaptor<AnalyticsRecord> captor = ArgumentCaptor.forClass(AnalyticsRecord.class);
        verify(analyticsRecordPort).save(captor.capture());
        assertThat(captor.getValue().getMetricName()).isEqualTo("page_views");
    }
}
