package com.example.customer.presentation.controller.integration;

import com.example.customer.application.port.outbound.CustomerRepositoryPort;
import com.example.customer.domain.model.Customer;
import com.example.customer.presentation.controller.dto.CreateCustomerRequest;
import com.example.customer.presentation.controller.dto.CustomerResponse;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest
class CustomerIntegrationTest {

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Inject
    CustomerRepositoryPort customerRepositoryPort;

    @MockBean(CustomerRepositoryPort.class)
    CustomerRepositoryPort mockCustomerRepositoryPort() {
        return mock(CustomerRepositoryPort.class);
    }

    @Test
    void shouldCreateCustomerViaHttp() {
        Customer savedCustomer = new Customer(1L, "John", "Doe", "john@example.com", "+1-555-0123", LocalDateTime.now());
        when(customerRepositoryPort.save(any(Customer.class))).thenReturn(savedCustomer);

        CreateCustomerRequest request = new CreateCustomerRequest("John", "Doe", "john@example.com", "+1-555-0123");
        HttpResponse<CustomerResponse> response = httpClient.toBlocking().exchange(
                HttpRequest.POST("/customers", request).header("X-Request-ID", "test-123"),
                CustomerResponse.class
        );

        assertThat((Object) response.getStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.body()).isNotNull();
        assertThat(response.body().firstName()).isEqualTo("John");
        assertThat(response.body().email()).isEqualTo("john@example.com");
    }

    @Test
    void shouldGetCustomerByIdViaHttp() {
        Customer customer = new Customer(1L, "John", "Doe", "john@example.com", "+1-555-0123", LocalDateTime.now());
        when(customerRepositoryPort.findById(1L)).thenReturn(Optional.of(customer));

        HttpResponse<CustomerResponse> response = httpClient.toBlocking().exchange(
                HttpRequest.GET("/customers/1"),
                CustomerResponse.class
        );

        assertThat((Object) response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.body()).isNotNull();
        assertThat(response.body().lastName()).isEqualTo("Doe");
    }

    @Test
    void shouldReturn404WhenCustomerNotFoundViaHttp() {
        when(customerRepositoryPort.findById(999L)).thenReturn(Optional.empty());

        HttpClientResponseException exception = catchThrowableOfType(
                () -> httpClient.toBlocking().exchange(HttpRequest.GET("/customers/999"), CustomerResponse.class),
                HttpClientResponseException.class
        );

        assertThat((Object) exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
