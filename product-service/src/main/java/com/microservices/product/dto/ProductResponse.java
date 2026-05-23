package com.microservices.product.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private Long id;

    @JsonProperty("nombre")
    private String name;

    @JsonProperty("precio")
    private BigDecimal price;

    @JsonProperty("stock")
    private Integer stock;
}
