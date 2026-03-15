package com.example.order.integration;

import com.example.order.application.port.outbound.OrderRepositoryPort;
import com.example.order.domain.model.Order;
import com.example.order.presentation.controller.dto.CreateOrderRequest;
import com.example.order.presentation.controller.dto.OrderResponse;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest
class OrderIntegrationTest {

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Inject
    OrderRepositoryPort orderRepositoryPort;

    @MockBean(OrderRepositoryPort.class)
    OrderRepositoryPort mockOrderRepositoryPort() {
        return mock(OrderRepositoryPort.class);
    }

    @Test
    void createOrder_shouldReturn201() {
        Order savedOrder = new Order(1L, 100L, "Keyboard", 2, new BigDecimal("49.99"), "PENDING", LocalDateTime.now());
        when(orderRepositoryPort.save(any(Order.class))).thenReturn(savedOrder);

        CreateOrderRequest request = new CreateOrderRequest(100L, "Keyboard", 2, new BigDecimal("49.99"));
        HttpRequest<?> httpRequest = HttpRequest.POST("/orders", request)
                .header("Authorization", "Bearer test-token")
                .header("X-Correlation-ID", "test-corr-id");

        HttpResponse<OrderResponse> response = httpClient.toBlocking().exchange(httpRequest, OrderResponse.class);

        assertThat((Object) response.status()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.body()).isNotNull();
        assertThat(response.body().id()).isEqualTo(1L);
    }

    @Test
    void getOrderById_shouldReturn200_whenFound() {
        Order order = new Order(1L, 100L, "Keyboard", 2, new BigDecimal("49.99"), "PENDING", LocalDateTime.now());
        when(orderRepositoryPort.findById(1L)).thenReturn(Optional.of(order));

        HttpRequest<?> httpRequest = HttpRequest.GET("/orders/1")
                .header("Authorization", "Bearer test-token")
                .header("X-Correlation-ID", "test-corr-id");

        HttpResponse<OrderResponse> response = httpClient.toBlocking().exchange(httpRequest, OrderResponse.class);

        assertThat((Object) response.status()).isEqualTo(HttpStatus.OK);
        assertThat(response.body()).isNotNull();
        assertThat(response.body().productName()).isEqualTo("Keyboard");
    }

    @Test
    void getAllOrders_shouldReturn200() {
        List<Order> orders = List.of(
                new Order(1L, 100L, "Keyboard", 2, new BigDecimal("49.99"), "PENDING", LocalDateTime.now())
        );
        when(orderRepositoryPort.findAll()).thenReturn(orders);

        HttpRequest<?> httpRequest = HttpRequest.GET("/orders")
                .header("Authorization", "Bearer test-token")
                .header("X-Correlation-ID", "test-corr-id");

        HttpResponse<String> response = httpClient.toBlocking().exchange(httpRequest, String.class);

        assertThat((Object) response.status()).isEqualTo(HttpStatus.OK);
    }
}
