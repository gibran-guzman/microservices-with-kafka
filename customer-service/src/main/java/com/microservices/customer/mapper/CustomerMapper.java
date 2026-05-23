package com.microservices.customer.mapper;

import com.microservices.customer.dto.CustomerRequest;
import com.microservices.customer.dto.CustomerResponse;
import com.microservices.customer.entity.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public Customer toEntity(CustomerRequest request) {
        return Customer.builder()
                .name(request.getNombre())
                .email(request.getCorreo())
                .build();
    }

    public CustomerResponse toResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .nombre(customer.getName())
                .correo(customer.getEmail())
                .build();
    }

    public void updateEntity(Customer customer, CustomerRequest request) {
        customer.setName(request.getNombre());
        customer.setEmail(request.getCorreo());
    }
}
