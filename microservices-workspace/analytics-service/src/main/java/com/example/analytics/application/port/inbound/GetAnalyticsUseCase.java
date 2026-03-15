package com.example.analytics.application.port.inbound;

import com.example.analytics.domain.model.AnalyticsReport;
import com.example.analytics.domain.model.OrderData;

import java.util.List;

public interface GetAnalyticsUseCase {

    List<OrderData> getAllOrderData();

    AnalyticsReport generateReport();
}
