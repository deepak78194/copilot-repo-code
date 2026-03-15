package com.example.customer.application.usecase;

import com.example.customer.application.port.outbound.CustomerRepositoryPort;
import com.example.customer.domain.model.Customer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateCustomerServiceTest {

    @Mock
    private CustomerRepositoryPort customerRepositoryPort;

    @InjectMocks
    private CreateCustomerService createCustomerService;

    @Test
    void shouldCreateCustomerWithCorrectFields() {
        when(customerRepositoryPort.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer c = invocation.getArgument(0);
            c.setId(1L);
            return c;
        });

        Customer result = createCustomerService.createCustomer("John", "Doe", "john@example.com", "+1-555-0123");

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        assertThat(result.getPhone()).isEqualTo("+1-555-0123");
        assertThat(result.getCreatedAt()).isNotNull();

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepositoryPort).save(captor.capture());
        assertThat(captor.getValue().getCreatedAt()).isNotNull();
    }
}
