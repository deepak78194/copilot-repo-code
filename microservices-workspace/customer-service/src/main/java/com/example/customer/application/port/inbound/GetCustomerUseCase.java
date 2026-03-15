package com.example.customer.application.port.inbound;

import com.example.customer.domain.model.Customer;

import java.util.List;
import java.util.Optional;

public interface GetCustomerUseCase {

    Optional<Customer> getCustomerById(Long id);

    List<Customer> getAllCustomers();
}
