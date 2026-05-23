package com.microservices.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequest {

    @JsonProperty("clienteId")
    @NotNull(message = "El ID del cliente es obligatorio")
    private Long customerId;

    @JsonProperty("productoId")
    @NotNull(message = "El ID del producto es obligatorio")
    private Long productId;

    @JsonProperty("cantidad")
    @Positive(message = "La cantidad debe ser mayor a cero")
    private int quantity;
}
