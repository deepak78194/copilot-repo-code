package com.example.customer.infrastructure.persistence;

import com.example.customer.application.port.outbound.CustomerRepositoryPort;
import com.example.customer.domain.model.Customer;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Optional;

@Singleton
public class CustomerRepositoryAdapter implements CustomerRepositoryPort {

    private final CustomerStoredProcRepository storedProcRepository;

    public CustomerRepositoryAdapter(CustomerStoredProcRepository storedProcRepository) {
        this.storedProcRepository = storedProcRepository;
    }

    @Override
    public Customer save(Customer customer) {
        CustomerStoredProcRepository.CustomerRow row = storedProcRepository.createCustomer(
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getPhone()
        );
        return toDomain(row);
    }

    @Override
    public Optional<Customer> findById(Long id) {
        return storedProcRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Customer> findAll() {
        return storedProcRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    private Customer toDomain(CustomerStoredProcRepository.CustomerRow row) {
        return new Customer(
                row.id(),
                row.firstName(),
                row.lastName(),
                row.email(),
                row.phone(),
                row.createdAt()
        );
    }
}
