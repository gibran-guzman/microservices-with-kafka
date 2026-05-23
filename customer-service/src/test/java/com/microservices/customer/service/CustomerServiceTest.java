package com.microservices.customer.service;

import com.microservices.common.exception.DuplicateResourceException;
import com.microservices.common.exception.ResourceNotFoundException;
import com.microservices.customer.dto.CustomerRequest;
import com.microservices.customer.dto.CustomerResponse;
import com.microservices.customer.entity.Customer;
import com.microservices.customer.mapper.CustomerMapper;
import com.microservices.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository repository;
    @Mock
    private CustomerMapper mapper;

    private CustomerService service;

    @BeforeEach
    void setUp() {
        service = new CustomerService(repository, mapper);
    }

    @Test
    void create_shouldReturnCustomerResponse() {
        CustomerRequest request = new CustomerRequest("Juan Perez", "juan@test.com");
        Customer entity = Customer.builder().name("Juan Perez").email("juan@test.com").build();
        Customer saved = Customer.builder().id(1L).name("Juan Perez").email("juan@test.com").build();
        CustomerResponse response = CustomerResponse.builder().id(1L).name("Juan Perez").email("juan@test.com").build();

        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(response);

        CustomerResponse result = service.create(request);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Juan Perez");
        verify(repository).save(entity);
    }

    @Test
    void create_shouldThrowDuplicateResourceException_whenEmailExists() {
        CustomerRequest request = new CustomerRequest("Juan Perez", "juan@test.com");
        Customer entity = Customer.builder().name("Juan Perez").email("juan@test.com").build();

        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(entity)).thenThrow(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("correo");
    }

    @Test
    void findById_shouldReturnCustomerResponse() {
        Customer customer = Customer.builder().id(1L).name("Juan").email("juan@test.com").build();
        CustomerResponse response = CustomerResponse.builder().id(1L).name("Juan").email("juan@test.com").build();

        when(repository.findById(1L)).thenReturn(Optional.of(customer));
        when(mapper.toResponse(customer)).thenReturn(response);

        CustomerResponse result = service.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void findById_shouldThrowResourceNotFoundException_whenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void findAll_shouldReturnAllCustomers() {
        Customer c1 = Customer.builder().id(1L).name("Juan").build();
        Customer c2 = Customer.builder().id(2L).name("Maria").build();
        when(repository.findAll()).thenReturn(List.of(c1, c2));
        when(mapper.toResponse(c1)).thenReturn(CustomerResponse.builder().id(1L).name("Juan").build());
        when(mapper.toResponse(c2)).thenReturn(CustomerResponse.builder().id(2L).name("Maria").build());

        List<CustomerResponse> result = service.findAll();

        assertThat(result).hasSize(2);
    }

    @Test
    void update_shouldReturnUpdatedCustomer() {
        CustomerRequest request = new CustomerRequest("Juan Updated", "juan@test.com");
        Customer existing = Customer.builder().id(1L).name("Juan").email("juan@test.com").build();
        CustomerResponse response = CustomerResponse.builder().id(1L).name("Juan Updated").email("juan@test.com").build();

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        when(mapper.toResponse(existing)).thenReturn(response);

        CustomerResponse result = service.update(1L, request);

        assertThat(result.getName()).isEqualTo("Juan Updated");
    }

    @Test
    void update_shouldThrowDuplicateResourceException_whenEmailConflict() {
        CustomerRequest request = new CustomerRequest("Juan", "otro@test.com");
        Customer existing = Customer.builder().id(1L).name("Juan").email("juan@test.com").build();

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenThrow(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> service.update(1L, request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void delete_shouldDeleteCustomer() {
        Customer customer = Customer.builder().id(1L).build();
        when(repository.findById(1L)).thenReturn(Optional.of(customer));

        service.delete(1L);

        verify(repository).delete(customer);
    }

    @Test
    void delete_shouldThrowResourceNotFoundException_whenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
