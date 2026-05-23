package com.microservices.product.mapper;

import com.microservices.product.dto.ProductRequest;
import com.microservices.product.dto.ProductResponse;
import com.microservices.product.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toEntity(ProductRequest request) {
        return Product.builder()
                .name(request.getNombre())
                .price(request.getPrecio())
                .stock(request.getStock())
                .build();
    }

    public ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .nombre(product.getName())
                .precio(product.getPrice())
                .stock(product.getStock())
                .build();
    }

    public void updateEntity(Product product, ProductRequest request) {
        product.setName(request.getNombre());
        product.setPrice(request.getPrecio());
        product.setStock(request.getStock());
    }
}
