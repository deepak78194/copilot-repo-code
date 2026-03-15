package com.example.order.application.usecase;

import com.example.order.application.port.outbound.OrderRepositoryPort;
import com.example.order.domain.model.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateOrderServiceTest {

    @Mock
    private OrderRepositoryPort orderRepositoryPort;

    private CreateOrderService createOrderService;

    @BeforeEach
    void setUp() {
        createOrderService = new CreateOrderService(orderRepositoryPort);
    }

    @Test
    void createOrder_shouldSaveAndReturnOrder() {
        when(orderRepositoryPort.save(any(Order.class))).thenAnswer(invocation -> {
            Order input = invocation.getArgument(0);
            input.setId(1L);
            return input;
        });

        Order result = createOrderService.createOrder("Keyboard", 100L, 2, new BigDecimal("49.99"));

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getProductName()).isEqualTo("Keyboard");
        assertThat(result.getCustomerId()).isEqualTo(100L);
        assertThat(result.getQuantity()).isEqualTo(2);
        assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("49.99"));
        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getCreatedAt()).isNotNull();
        verify(orderRepositoryPort).save(any(Order.class));
    }
}
