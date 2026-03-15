package com.example.customer.application.port.outbound;

import com.example.customer.domain.model.Customer;

import java.util.List;
import java.util.Optional;

public interface CustomerRepositoryPort {

    Customer save(Customer customer);

    Optional<Customer> findById(Long id);

    List<Customer> findAll();
}
