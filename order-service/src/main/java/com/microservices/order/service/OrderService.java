package com.microservices.order.service;

import com.microservices.order.dto.OrderRequest;
import com.microservices.order.dto.OrderResponse;
import com.microservices.order.entity.Order;
import com.microservices.order.event.OrderEvent;
import com.microservices.order.event.OrderEventProducer;
import com.microservices.order.exception.ResourceNotFoundException;
import com.microservices.order.mapper.OrderMapper;
import com.microservices.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository repository;
    private final OrderMapper mapper;
    private final OrderEventProducer eventProducer;

    public OrderResponse create(OrderRequest request) {
        log.info("Creando pedido para clienteId: {}, productoId: {}",
                request.getClienteId(), request.getProductoId());

        Order order = mapper.toEntity(request);
        order = repository.save(order);

        OrderEvent event = OrderEvent.builder()
                .orderId(order.getId())
                .customerId(order.getCustomerId())
                .productId(order.getProductId())
                .quantity(order.getQuantity())
                .timestamp(LocalDateTime.now())
                .build();

        eventProducer.publish(event);

        log.info("Pedido creado exitosamente con id: {}", order.getId());
        return mapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll() {
        log.info("Consultando todos los pedidos");
        return repository.findAll()
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        log.info("Consultando pedido con id: {}", id);
        Order order = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pedido no encontrado con id: " + id));
        return mapper.toResponse(order);
    }
}
