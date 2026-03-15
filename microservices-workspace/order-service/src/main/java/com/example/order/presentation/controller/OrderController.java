package com.example.order.presentation.controller;

import com.example.order.application.port.inbound.CreateOrderUseCase;
import com.example.order.application.port.inbound.GetOrderUseCase;
import com.example.order.domain.model.Order;
import com.example.order.presentation.controller.dto.CreateOrderRequest;
import com.example.order.presentation.controller.dto.OrderResponse;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Status;
import io.micronaut.validation.Validated;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.List;

@Controller("/orders")
@Validated
@Tag(name = "Orders", description = "Operations for managing orders")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;

    public OrderController(CreateOrderUseCase createOrderUseCase, GetOrderUseCase getOrderUseCase) {
        this.createOrderUseCase = createOrderUseCase;
        this.getOrderUseCase = getOrderUseCase;
    }

    @Post
    @Status(HttpStatus.CREATED)
    @Operation(
            summary = "Create a new order",
            description = "Creates a new order with the provided details and returns the created order with its generated ID"
    )
    @ApiResponse(responseCode = "201", description = "Order created successfully",
            content = @Content(schema = @Schema(implementation = OrderResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request payload")
    public OrderResponse createOrder(
            @Parameter(in = ParameterIn.HEADER, description = "Bearer token for authentication", required = true)
            @Header("Authorization") String authorization,
            @Parameter(in = ParameterIn.HEADER, description = "Correlation ID for request tracing")
            @Header(value = "X-Correlation-ID", defaultValue = "none") String correlationId,
            @Body @Valid CreateOrderRequest request
    ) {
        Order order = createOrderUseCase.createOrder(
                request.productName(),
                request.customerId(),
                request.quantity(),
                request.totalAmount()
        );
        return OrderResponse.fromDomain(order);
    }

    @Get("/{id}")
    @Operation(
            summary = "Get order by ID",
            description = "Retrieves a single order by its unique identifier"
    )
    @ApiResponse(responseCode = "200", description = "Order found",
            content = @Content(schema = @Schema(implementation = OrderResponse.class)))
    @ApiResponse(responseCode = "404", description = "Order not found")
    public HttpResponse<OrderResponse> getOrderById(
            @Parameter(in = ParameterIn.HEADER, description = "Bearer token for authentication", required = true)
            @Header("Authorization") String authorization,
            @Parameter(in = ParameterIn.HEADER, description = "Correlation ID for request tracing")
            @Header(value = "X-Correlation-ID", defaultValue = "none") String correlationId,
            @Parameter(description = "Unique order identifier") @PathVariable Long id
    ) {
        return getOrderUseCase.getOrderById(id)
                .map(order -> HttpResponse.ok(OrderResponse.fromDomain(order)))
                .orElse(HttpResponse.notFound());
    }

    @Get
    @Operation(
            summary = "List all orders",
            description = "Retrieves all orders sorted by creation date in descending order"
    )
    @ApiResponse(responseCode = "200", description = "List of orders retrieved successfully",
            content = @Content(schema = @Schema(implementation = OrderResponse.class)))
    public List<OrderResponse> getAllOrders(
            @Parameter(in = ParameterIn.HEADER, description = "Bearer token for authentication", required = true)
            @Header("Authorization") String authorization,
            @Parameter(in = ParameterIn.HEADER, description = "Correlation ID for request tracing")
            @Header(value = "X-Correlation-ID", defaultValue = "none") String correlationId
    ) {
        return getOrderUseCase.getAllOrders().stream()
                .map(OrderResponse::fromDomain)
                .toList();
    }
}
