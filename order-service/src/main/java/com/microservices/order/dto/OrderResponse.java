package com.microservices.order.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    @JsonProperty("total")
    private BigDecimal totalAmount;

    @JsonProperty("estado")
    private String status;

    @JsonProperty("fechaCreacion")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
