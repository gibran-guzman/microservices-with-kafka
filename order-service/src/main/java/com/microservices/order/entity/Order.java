package com.microservices.order.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonProperty("clienteId")
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @JsonProperty("productoId")
    @Column(name = "product_id", nullable = false)
    private Long productId;

    @JsonProperty("cantidad")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
}
