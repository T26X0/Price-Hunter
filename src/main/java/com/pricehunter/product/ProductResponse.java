package com.pricehunter.product;

import java.time.Instant;
import java.util.UUID;

public record ProductResponse(UUID id, String name, String sku, String description, Instant createdAt) {

    static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getDescription(),
                product.getCreatedAt()
        );
    }

    static ProductResponse from(ProductSummaryProjection product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getDescription(),
                product.getCreatedAt()
        );
    }
}
