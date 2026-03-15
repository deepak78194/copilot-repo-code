package com.example.analytics.application.port.outbound;

import com.example.analytics.domain.model.AnalyticsRecord;

import java.util.List;

public interface AnalyticsRecordPort {

    AnalyticsRecord save(AnalyticsRecord record);

    List<AnalyticsRecord> findAll();

    long count();
}
