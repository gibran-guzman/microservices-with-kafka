package com.microservices.customer.service;

import com.microservices.common.exception.DuplicateResourceException;
import com.microservices.common.exception.ResourceNotFoundException;
import com.microservices.customer.dto.CustomerRequest;
import com.microservices.customer.dto.CustomerResponse;
import com.microservices.customer.entity.Customer;
import com.microservices.customer.mapper.CustomerMapper;
import com.microservices.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomerService {

    private final CustomerRepository repository;
    private final CustomerMapper mapper;

    public CustomerResponse create(CustomerRequest request) {
        log.info("Creando cliente con correo: {}", request.getEmail());

        Customer customer = mapper.toEntity(request);
        try {
            customer = repository.save(customer);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateResourceException(
                    "Ya existe un cliente con el correo: " + request.getEmail());
        }

        log.info("Cliente creado exitosamente con id: {}", customer.getId());
        return mapper.toResponse(customer);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll() {
        log.info("Consultando todos los clientes");
        return repository.findAll()
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> findAll(Pageable pageable) {
        log.info("Consultando clientes paginados: {}", pageable);
        return repository.findAll(pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(Long id) {
        log.info("Consultando cliente con id: {}", id);
        Customer customer = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cliente no encontrado con id: " + id));
        return mapper.toResponse(customer);
    }

    public CustomerResponse update(Long id, CustomerRequest request) {
        log.info("Actualizando cliente con id: {}", id);
        Customer customer = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cliente no encontrado con id: " + id));

        boolean emailChanged = !customer.getEmail().equals(request.getEmail());
        mapper.updateEntity(customer, request);
        try {
            customer = repository.save(customer);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateResourceException(
                    "Ya existe un cliente con el correo: " + request.getEmail());
        }

        log.info("Cliente actualizado exitosamente con id: {}", id);
        return mapper.toResponse(customer);
    }

    public void delete(Long id) {
        log.info("Eliminando cliente con id: {}", id);
        Customer customer = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cliente no encontrado con id: " + id));
        repository.delete(customer);
        log.info("Cliente eliminado exitosamente con id: {}", id);
    }
}
