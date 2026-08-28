package com.pricehunter.product;

import java.time.Instant;
import java.util.UUID;

/** Публичное представление модели товара в REST API. */
public record ProductResponse(UUID id, String name, String sku, String description, Instant createdAt) {

    /** Преобразует полную JPA-сущность в DTO. */
    static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getDescription(),
                product.getCreatedAt()
        );
    }

    /** Преобразует узкую проекцию запроса в DTO без дополнительного чтения базы. */
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
