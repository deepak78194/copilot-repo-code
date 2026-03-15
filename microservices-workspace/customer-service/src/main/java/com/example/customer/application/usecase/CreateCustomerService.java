package com.example.customer.application.usecase;

import com.example.customer.application.port.inbound.CreateCustomerUseCase;
import com.example.customer.application.port.outbound.CustomerRepositoryPort;
import com.example.customer.domain.model.Customer;
import jakarta.inject.Singleton;

import java.time.LocalDateTime;

@Singleton
public class CreateCustomerService implements CreateCustomerUseCase {

    private final CustomerRepositoryPort customerRepositoryPort;

    public CreateCustomerService(CustomerRepositoryPort customerRepositoryPort) {
        this.customerRepositoryPort = customerRepositoryPort;
    }

    @Override
    public Customer createCustomer(String firstName, String lastName, String email, String phone) {
        Customer customer = new Customer();
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setEmail(email);
        customer.setPhone(phone);
        customer.setCreatedAt(LocalDateTime.now());
        return customerRepositoryPort.save(customer);
    }
}
