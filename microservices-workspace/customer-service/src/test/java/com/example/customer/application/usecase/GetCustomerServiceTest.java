package com.example.customer.application.usecase;

import com.example.customer.application.port.outbound.CustomerRepositoryPort;
import com.example.customer.domain.model.Customer;
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
class GetCustomerServiceTest {

    @Mock
    private CustomerRepositoryPort customerRepositoryPort;

    @InjectMocks
    private GetCustomerService getCustomerService;

    @Test
    void shouldReturnCustomerWhenFound() {
        Customer customer = new Customer(1L, "John", "Doe", "john@example.com", "+1-555-0123", LocalDateTime.now());
        when(customerRepositoryPort.findById(1L)).thenReturn(Optional.of(customer));

        Optional<Customer> result = getCustomerService.getCustomerById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getFirstName()).isEqualTo("John");
    }

    @Test
    void shouldReturnEmptyWhenCustomerNotFound() {
        when(customerRepositoryPort.findById(999L)).thenReturn(Optional.empty());

        Optional<Customer> result = getCustomerService.getCustomerById(999L);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnAllCustomers() {
        List<Customer> customers = List.of(
                new Customer(1L, "John", "Doe", "john@example.com", "+1-555-0123", LocalDateTime.now()),
                new Customer(2L, "Jane", "Smith", "jane@example.com", "+1-555-0456", LocalDateTime.now())
        );
        when(customerRepositoryPort.findAll()).thenReturn(customers);

        List<Customer> result = getCustomerService.getAllCustomers();

        assertThat(result).hasSize(2);
    }
}
