package com.example.customer.presentation.controller;

import com.example.customer.application.port.inbound.CreateCustomerUseCase;
import com.example.customer.application.port.inbound.GetCustomerUseCase;
import com.example.customer.domain.model.Customer;
import com.example.customer.presentation.controller.dto.CreateCustomerRequest;
import com.example.customer.presentation.controller.dto.CustomerResponse;
import io.micronaut.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    @Mock
    private CreateCustomerUseCase createCustomerUseCase;

    @Mock
    private GetCustomerUseCase getCustomerUseCase;

    @InjectMocks
    private CustomerController customerController;

    @Test
    void shouldCreateCustomerAndReturn201() {
        Customer customer = new Customer(1L, "John", "Doe", "john@example.com", "+1-555-0123", LocalDateTime.now());
        when(createCustomerUseCase.createCustomer("John", "Doe", "john@example.com", "+1-555-0123"))
                .thenReturn(customer);

        CreateCustomerRequest request = new CreateCustomerRequest("John", "Doe", "john@example.com", "+1-555-0123");
        HttpResponse<CustomerResponse> response = customerController.createCustomer(request, "req-123");

        assertThat(response.getStatus().getCode()).isEqualTo(201);
        assertThat(response.body()).isNotNull();
        assertThat(response.body().firstName()).isEqualTo("John");
    }

    @Test
    void shouldReturnCustomerWhenFoundById() {
        Customer customer = new Customer(1L, "John", "Doe", "john@example.com", "+1-555-0123", LocalDateTime.now());
        when(getCustomerUseCase.getCustomerById(1L)).thenReturn(Optional.of(customer));

        HttpResponse<CustomerResponse> response = customerController.getCustomerById(1L);

        assertThat(response.getStatus().getCode()).isEqualTo(200);
        assertThat(response.body()).isNotNull();
        assertThat(response.body().email()).isEqualTo("john@example.com");
    }

    @Test
    void shouldReturn404WhenCustomerNotFound() {
        when(getCustomerUseCase.getCustomerById(999L)).thenReturn(Optional.empty());

        HttpResponse<CustomerResponse> response = customerController.getCustomerById(999L);

        assertThat(response.getStatus().getCode()).isEqualTo(404);
    }

    @Test
    void shouldReturnAllCustomers() {
        List<Customer> customers = List.of(
                new Customer(1L, "John", "Doe", "john@example.com", "+1-555-0123", LocalDateTime.now()),
                new Customer(2L, "Jane", "Smith", "jane@example.com", "+1-555-0456", LocalDateTime.now())
        );
        when(getCustomerUseCase.getAllCustomers()).thenReturn(customers);

        HttpResponse<List<CustomerResponse>> response = customerController.getAllCustomers();

        assertThat(response.getStatus().getCode()).isEqualTo(200);
        assertThat(response.body()).hasSize(2);
    }
}
