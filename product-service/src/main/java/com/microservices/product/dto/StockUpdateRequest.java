package com.microservices.product.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockUpdateRequest {

    @JsonProperty("cantidad")
    @Positive(message = "La cantidad debe ser mayor a cero")
    private int quantity;
}
