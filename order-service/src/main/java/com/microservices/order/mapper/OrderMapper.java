package com.microservices.order.mapper;

import com.microservices.order.dto.OrderRequest;
import com.microservices.order.dto.OrderResponse;
import com.microservices.order.entity.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public Order toEntity(OrderRequest request) {
        return Order.builder()
                .customerId(request.getClienteId())
                .productId(request.getProductoId())
                .quantity(request.getCantidad())
                .build();
    }

    public OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .clienteId(order.getCustomerId())
                .productoId(order.getProductId())
                .cantidad(order.getQuantity())
                .build();
    }
}
