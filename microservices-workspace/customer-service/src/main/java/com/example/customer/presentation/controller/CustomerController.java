package com.example.customer.presentation.controller;

import com.example.customer.application.port.inbound.CreateCustomerUseCase;
import com.example.customer.application.port.inbound.GetCustomerUseCase;
import com.example.customer.domain.model.Customer;
import com.example.customer.presentation.controller.dto.CreateCustomerRequest;
import com.example.customer.presentation.controller.dto.CustomerResponse;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.validation.Validated;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.net.URI;
import java.util.List;

@Controller("/customers")
@Validated
@Tag(name = "Customers", description = "Customer management operations via Oracle stored procedures")
public class CustomerController {

    private final CreateCustomerUseCase createCustomerUseCase;
    private final GetCustomerUseCase getCustomerUseCase;

    public CustomerController(CreateCustomerUseCase createCustomerUseCase,
                              GetCustomerUseCase getCustomerUseCase) {
        this.createCustomerUseCase = createCustomerUseCase;
        this.getCustomerUseCase = getCustomerUseCase;
    }

    @Post
    @Operation(summary = "Create a new customer", description = "Creates a new customer using Oracle stored procedure")
    @ApiResponse(responseCode = "201", description = "Customer created successfully",
            content = @Content(schema = @Schema(implementation = CustomerResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request payload")
    public HttpResponse<CustomerResponse> createCustomer(
            @Body @Valid CreateCustomerRequest request,
            @Header("X-Request-ID") @Parameter(description = "Request correlation ID") String requestId) {
        Customer customer = createCustomerUseCase.createCustomer(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phone()
        );
        CustomerResponse response = CustomerResponse.fromDomain(customer);
        return HttpResponse.created(response, URI.create("/customers/" + customer.getId()));
    }

    @Get("/{id}")
    @Operation(summary = "Get customer by ID", description = "Retrieves a customer by their unique identifier")
    @ApiResponse(responseCode = "200", description = "Customer found",
            content = @Content(schema = @Schema(implementation = CustomerResponse.class)))
    @ApiResponse(responseCode = "404", description = "Customer not found")
    public HttpResponse<CustomerResponse> getCustomerById(
            @PathVariable @Parameter(description = "Customer unique identifier") Long id) {
        return getCustomerUseCase.getCustomerById(id)
                .map(customer -> HttpResponse.ok(CustomerResponse.fromDomain(customer)))
                .orElse(HttpResponse.notFound());
    }

    @Get
    @Operation(summary = "Get all customers", description = "Retrieves all customers from the database")
    @ApiResponse(responseCode = "200", description = "List of all customers",
            content = @Content(schema = @Schema(implementation = CustomerResponse.class)))
    public HttpResponse<List<CustomerResponse>> getAllCustomers() {
        List<CustomerResponse> responses = getCustomerUseCase.getAllCustomers().stream()
                .map(CustomerResponse::fromDomain)
                .toList();
        return HttpResponse.ok(responses);
    }
}
