package com.example.order.application.usecase;

import com.example.order.application.port.outbound.OrderRepositoryPort;
import com.example.order.domain.model.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetOrderServiceTest {

    @Mock
    private OrderRepositoryPort orderRepositoryPort;

    private GetOrderService getOrderService;

    @BeforeEach
    void setUp() {
        getOrderService = new GetOrderService(orderRepositoryPort);
    }

    @Test
    void getOrderById_shouldReturnOrder_whenExists() {
        Order order = new Order(1L, 100L, "Keyboard", 2, new BigDecimal("49.99"), "PENDING", LocalDateTime.now());
        when(orderRepositoryPort.findById(1L)).thenReturn(Optional.of(order));

        Optional<Order> result = getOrderService.getOrderById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    void getOrderById_shouldReturnEmpty_whenNotFound() {
        when(orderRepositoryPort.findById(999L)).thenReturn(Optional.empty());

        Optional<Order> result = getOrderService.getOrderById(999L);

        assertThat(result).isEmpty();
    }

    @Test
    void getAllOrders_shouldReturnAllOrders() {
        List<Order> orders = List.of(
                new Order(1L, 100L, "Keyboard", 2, new BigDecimal("49.99"), "PENDING", LocalDateTime.now()),
                new Order(2L, 101L, "Mouse", 1, new BigDecimal("29.99"), "CONFIRMED", LocalDateTime.now())
        );
        when(orderRepositoryPort.findAll()).thenReturn(orders);

        List<Order> result = getOrderService.getAllOrders();

        assertThat(result).hasSize(2);
    }
}
