package com.microservices.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private Long id;
    private Long clienteId;
    private Long productoId;
    private Integer cantidad;
}
