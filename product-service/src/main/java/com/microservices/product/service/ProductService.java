package com.microservices.product.service;

import com.microservices.common.exception.DuplicateResourceException;
import com.microservices.common.exception.ResourceNotFoundException;
import com.microservices.product.dto.ProductRequest;
import com.microservices.product.dto.ProductResponse;
import com.microservices.product.entity.Product;
import com.microservices.product.mapper.ProductMapper;
import com.microservices.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductService {

    private final ProductRepository repository;
    private final ProductMapper mapper;

    public ProductResponse create(ProductRequest request) {
        log.info("Creando producto con nombre: {}", request.getName());

        if (repository.existsByName(request.getName())) {
            throw new DuplicateResourceException(
                    "Ya existe un producto con el nombre: " + request.getName());
        }

        Product product = mapper.toEntity(request);
        product = repository.save(product);

        log.info("Producto creado exitosamente con id: {}", product.getId());
        return mapper.toResponse(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        log.info("Consultando todos los productos");
        return repository.findAll()
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        log.info("Consultando producto con id: {}", id);
        Product product = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Producto no encontrado con id: " + id));
        return mapper.toResponse(product);
    }

    public ProductResponse update(Long id, ProductRequest request) {
        log.info("Actualizando producto con id: {}", id);
        Product product = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Producto no encontrado con id: " + id));

        if (!product.getName().equals(request.getName())
                && repository.existsByName(request.getName())) {
            throw new DuplicateResourceException(
                    "Ya existe un producto con el nombre: " + request.getName());
        }

        mapper.updateEntity(product, request);
        product = repository.save(product);

        log.info("Producto actualizado exitosamente con id: {}", id);
        return mapper.toResponse(product);
    }

    public void delete(Long id) {
        log.info("Eliminando producto con id: {}", id);
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException(
                    "Producto no encontrado con id: " + id);
        }
        repository.deleteById(id);
        log.info("Producto eliminado exitosamente con id: {}", id);
    }
}
