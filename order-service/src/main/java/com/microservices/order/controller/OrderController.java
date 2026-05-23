package com.microservices.order.controller;

import com.microservices.order.dto.OrderRequest;
import com.microservices.order.dto.OrderResponse;
import com.microservices.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/pedidos")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;

    @PostMapping
    public ResponseEntity<OrderResponse> create(
            @Valid @RequestBody OrderRequest request) {
        OrderResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> findAll() {
        List<OrderResponse> response = service.findAll();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> findById(@PathVariable Long id) {
        OrderResponse response = service.findById(id);
        return ResponseEntity.ok(response);
    }
}
