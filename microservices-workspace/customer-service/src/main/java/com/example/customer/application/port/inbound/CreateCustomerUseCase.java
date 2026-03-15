package com.example.customer.application.port.inbound;

import com.example.customer.domain.model.Customer;

public interface CreateCustomerUseCase {

    Customer createCustomer(String firstName, String lastName, String email, String phone);
}
