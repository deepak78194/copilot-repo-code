package com.example.customer.application.usecase;

import com.example.customer.application.port.inbound.GetCustomerUseCase;
import com.example.customer.application.port.outbound.CustomerRepositoryPort;
import com.example.customer.domain.model.Customer;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Optional;

@Singleton
public class GetCustomerService implements GetCustomerUseCase {

    private final CustomerRepositoryPort customerRepositoryPort;

    public GetCustomerService(CustomerRepositoryPort customerRepositoryPort) {
        this.customerRepositoryPort = customerRepositoryPort;
    }

    @Override
    public Optional<Customer> getCustomerById(Long id) {
        return customerRepositoryPort.findById(id);
    }

    @Override
    public List<Customer> getAllCustomers() {
        return customerRepositoryPort.findAll();
    }
}
