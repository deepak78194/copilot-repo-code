package com.example.analytics.application.port.outbound;

import com.example.analytics.domain.model.OrderData;

import java.math.BigDecimal;
import java.util.List;

public interface OrderDataPort {

    List<OrderData> findAll();

    long count();

    BigDecimal sumTotalAmount();
}
