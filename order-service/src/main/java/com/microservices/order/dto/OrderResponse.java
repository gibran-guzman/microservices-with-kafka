package com.microservices.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("clienteId")
    private Long customerId;

    @JsonProperty("productoId")
    private Long productId;

    @JsonProperty("cantidad")
    private Integer quantity;
}
