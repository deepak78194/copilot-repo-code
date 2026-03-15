package com.example.order.presentation.controller;

import com.example.order.application.port.inbound.CreateOrderUseCase;
import com.example.order.application.port.inbound.GetOrderUseCase;
import com.example.order.domain.model.Order;
import com.example.order.presentation.controller.dto.CreateOrderRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private CreateOrderUseCase createOrderUseCase;

    @Mock
    private GetOrderUseCase getOrderUseCase;

    private OrderController orderController;

    @BeforeEach
    void setUp() {
        orderController = new OrderController(createOrderUseCase, getOrderUseCase);
    }

    @Test
    void createOrder_shouldReturnCreatedOrder() {
        Order order = new Order(1L, 100L, "Keyboard", 2, new BigDecimal("49.99"), "PENDING", LocalDateTime.now());
        when(createOrderUseCase.createOrder(eq("Keyboard"), eq(100L), eq(2), any(BigDecimal.class)))
                .thenReturn(order);

        CreateOrderRequest request = new CreateOrderRequest(100L, "Keyboard", 2, new BigDecimal("49.99"));
        var response = orderController.createOrder("Bearer token", "corr-123", request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.productName()).isEqualTo("Keyboard");
        assertThat(response.status()).isEqualTo("PENDING");
    }

    @Test
    void getOrderById_shouldReturnOrder_whenFound() {
        Order order = new Order(1L, 100L, "Keyboard", 2, new BigDecimal("49.99"), "PENDING", LocalDateTime.now());
        when(getOrderUseCase.getOrderById(1L)).thenReturn(Optional.of(order));

        HttpResponse<?> response = orderController.getOrderById("Bearer token", "corr-123", 1L);

        assertThat((Object) response.status()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getOrderById_shouldReturnNotFound_whenMissing() {
        when(getOrderUseCase.getOrderById(999L)).thenReturn(Optional.empty());

        HttpResponse<?> response = orderController.getOrderById("Bearer token", "corr-123", 999L);

        assertThat((Object) response.status()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getAllOrders_shouldReturnList() {
        List<Order> orders = List.of(
                new Order(1L, 100L, "Keyboard", 2, new BigDecimal("49.99"), "PENDING", LocalDateTime.now())
        );
        when(getOrderUseCase.getAllOrders()).thenReturn(orders);

        var result = orderController.getAllOrders("Bearer token", "corr-123");

        assertThat(result).hasSize(1);
    }
}
