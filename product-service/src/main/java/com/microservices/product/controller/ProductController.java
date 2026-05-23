package com.microservices.product.controller;

import com.microservices.product.dto.ProductRequest;
import com.microservices.product.dto.ProductResponse;
import com.microservices.product.dto.StockUpdateRequest;
import com.microservices.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/productos")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;

    @PostMapping
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestBody ProductRequest request) {
        ProductResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> findAll(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        Page<ProductResponse> response = service.findAll(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> findById(@PathVariable Long id) {
        ProductResponse response = service.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse response = service.update(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<Void> decrementStock(
            @PathVariable Long id,
            @Valid @RequestBody StockUpdateRequest request) {
        service.decrementStock(id, request.getQuantity());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
