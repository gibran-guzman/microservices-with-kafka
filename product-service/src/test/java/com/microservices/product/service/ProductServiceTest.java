package com.microservices.product.service;

import com.microservices.common.exception.DuplicateResourceException;
import com.microservices.common.exception.ResourceNotFoundException;
import com.microservices.product.dto.ProductRequest;
import com.microservices.product.dto.ProductResponse;
import com.microservices.product.entity.Product;
import com.microservices.product.mapper.ProductMapper;
import com.microservices.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository repository;
    @Mock
    private ProductMapper mapper;

    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(repository, mapper);
    }

    @Test
    void create_shouldReturnProductResponse() {
        ProductRequest request = new ProductRequest("Laptop", BigDecimal.valueOf(1000), 10);
        Product entity = Product.builder().name("Laptop").price(BigDecimal.valueOf(1000)).stock(10).build();
        Product saved = Product.builder().id(1L).name("Laptop").price(BigDecimal.valueOf(1000)).stock(10).build();
        ProductResponse response = ProductResponse.builder().id(1L).name("Laptop").price(BigDecimal.valueOf(1000)).stock(10).build();

        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(response);

        ProductResponse result = service.create(request);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Laptop");
        verify(repository).save(entity);
    }

    @Test
    void create_shouldThrowDuplicateResourceException_whenNameExists() {
        ProductRequest request = new ProductRequest("Laptop", BigDecimal.valueOf(1000), 10);
        Product entity = Product.builder().name("Laptop").build();

        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(entity)).thenThrow(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("nombre");
    }

    @Test
    void findById_shouldReturnProductResponse() {
        Product product = Product.builder().id(1L).name("Laptop").price(BigDecimal.TEN).stock(5).build();
        ProductResponse response = ProductResponse.builder().id(1L).name("Laptop").price(BigDecimal.TEN).stock(5).build();

        when(repository.findById(1L)).thenReturn(Optional.of(product));
        when(mapper.toResponse(product)).thenReturn(response);

        ProductResponse result = service.findById(1L);

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
    void findAll_shouldReturnAllProducts() {
        Product p1 = Product.builder().id(1L).name("Laptop").build();
        Product p2 = Product.builder().id(2L).name("Mouse").build();
        when(repository.findAll()).thenReturn(List.of(p1, p2));
        when(mapper.toResponse(p1)).thenReturn(ProductResponse.builder().id(1L).name("Laptop").build());
        when(mapper.toResponse(p2)).thenReturn(ProductResponse.builder().id(2L).name("Mouse").build());

        List<ProductResponse> result = service.findAll();

        assertThat(result).hasSize(2);
    }

    @Test
    void update_shouldReturnUpdatedProduct() {
        ProductRequest request = new ProductRequest("Laptop Pro", BigDecimal.valueOf(1500), 8);
        Product existing = Product.builder().id(1L).name("Laptop").price(BigDecimal.TEN).stock(5).build();
        ProductResponse response = ProductResponse.builder().id(1L).name("Laptop Pro").price(BigDecimal.valueOf(1500)).stock(8).build();

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        when(mapper.toResponse(existing)).thenReturn(response);

        ProductResponse result = service.update(1L, request);

        assertThat(result.getName()).isEqualTo("Laptop Pro");
    }

    @Test
    void update_shouldThrowDuplicateResourceException_whenNameConflict() {
        ProductRequest request = new ProductRequest("Otro", BigDecimal.TEN, 5);
        Product existing = Product.builder().id(1L).name("Laptop").price(BigDecimal.TEN).stock(5).build();

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenThrow(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> service.update(1L, request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void delete_shouldDeleteProduct() {
        Product product = Product.builder().id(1L).build();
        when(repository.findById(1L)).thenReturn(Optional.of(product));

        service.delete(1L);

        verify(repository).delete(product);
    }

    @Test
    void delete_shouldThrowResourceNotFoundException_whenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
