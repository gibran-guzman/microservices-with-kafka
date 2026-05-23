package com.microservices.order.service;

import com.microservices.common.exception.ResourceNotFoundException;
import com.microservices.order.dto.OrderRequest;
import com.microservices.order.dto.OrderResponse;
import com.microservices.order.entity.Order;
import com.microservices.order.event.OrderEvent;
import com.microservices.order.event.OrderEventProducer;
import com.microservices.order.mapper.OrderMapper;
import com.microservices.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository repository;
    private final OrderMapper mapper;
    private final OrderEventProducer eventProducer;
    private final RestClient.Builder restClientBuilder;

    public OrderResponse create(OrderRequest request) {
        log.info("Creando pedido para customerId: {}, productId: {}",
                request.getCustomerId(), request.getProductId());

        validateCustomerExists(request.getCustomerId());
        Map<String, Object> product = validateProductExists(request.getProductId());
        validateStock(product, request.getQuantity());
        BigDecimal unitPrice = getProductPrice(product);

        Order order = mapper.toEntity(request);
        order.setTotalAmount(unitPrice.multiply(BigDecimal.valueOf(request.getQuantity())));
        order.setStatus("CREADO");
        order.setCreatedAt(LocalDateTime.now());
        order = repository.save(order);

        decrementStock(request.getProductId(), request.getQuantity());

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
    public Page<OrderResponse> findAll(Pageable pageable) {
        log.info("Consultando pedidos paginados: {}", pageable);
        return repository.findAll(pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        log.info("Consultando pedido con id: {}", id);
        Order order = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pedido no encontrado con id: " + id));
        return mapper.toResponse(order);
    }

    private void validateCustomerExists(Long customerId) {
        RestClient client = restClientBuilder.build();
        try {
            client.get()
                    .uri("http://CUSTOMER-SERVICE/clientes/{id}", customerId)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Cliente {} validado exitosamente", customerId);
        } catch (Exception e) {
            log.error("Error validando cliente {}: {}", customerId, e.getMessage());
            throw new ResourceNotFoundException(
                    "Cliente no encontrado con id: " + customerId);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> validateProductExists(Long productId) {
        RestClient client = restClientBuilder.build();
        try {
            Map<String, Object> product = client.get()
                    .uri("http://PRODUCT-SERVICE/productos/{id}", productId)
                    .retrieve()
                    .body(Map.class);
            log.debug("Producto {} validado exitosamente", productId);
            return product;
        } catch (Exception e) {
            log.error("Error validando producto {}: {}", productId, e.getMessage());
            throw new ResourceNotFoundException(
                    "Producto no encontrado con id: " + productId);
        }
    }

    public void delete(Long id) {
        log.info("Eliminando pedido con id: {}", id);
        Order order = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pedido no encontrado con id: " + id));
        repository.delete(order);
        log.info("Pedido eliminado exitosamente con id: {}", id);
    }

    private BigDecimal getProductPrice(Map<String, Object> product) {
        Object precioObj = product.get("precio");
        if (precioObj == null) {
            throw new ResourceNotFoundException("Producto sin información de precio");
        }
        if (precioObj instanceof Number) {
            return BigDecimal.valueOf(((Number) precioObj).doubleValue());
        }
        return new BigDecimal(precioObj.toString());
    }

    private void validateStock(Map<String, Object> product, int requestedQuantity) {
        Object stockObj = product.get("stock");
        if (stockObj == null) {
            throw new ResourceNotFoundException("Producto sin información de stock");
        }
        int availableStock = ((Number) stockObj).intValue();
        if (requestedQuantity > availableStock) {
            throw new IllegalArgumentException(
                    "Stock insuficiente. Disponible: " + availableStock +
                    ", solicitado: " + requestedQuantity);
        }
    }

    private void decrementStock(Long productId, int quantity) {
        RestClient client = restClientBuilder.build();
        try {
            Map<String, Object> body = Map.of("cantidad", quantity);
            client.patch()
                    .uri("http://PRODUCT-SERVICE/productos/{id}/stock", productId)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Stock actualizado para producto: {}", productId);
        } catch (Exception e) {
            log.error("Error actualizando stock para producto {}: {}", productId, e.getMessage());
            throw new RuntimeException("Error al actualizar stock del producto: " + productId, e);
        }
    }
}
