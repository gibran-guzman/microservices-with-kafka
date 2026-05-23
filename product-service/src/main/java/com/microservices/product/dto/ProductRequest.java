package com.microservices.product.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {

    @JsonProperty("nombre")
    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    @JsonProperty("precio")
    @Positive(message = "El precio debe ser mayor a cero")
    private BigDecimal price;

    @JsonProperty("stock")
    @PositiveOrZero(message = "El stock no puede ser negativo")
    private Integer stock;
}
