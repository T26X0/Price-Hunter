package com.pricehunter.product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/** Лёгкая карточка модели товара в результатах поиска каталога. */
@Schema(description = "Краткая модель товара в каталоге")
public record ProductListResponse(
        UUID id,
        @Schema(example = "APPLE-IPHONE-16-PRO") String catalogKey,
        @Schema(example = "Apple") String brand,
        @Schema(example = "iPhone 16 Pro") String name,
        @Schema(example = "smartphones") String category,
        String description,
        BigDecimal minEffectivePrice,
        @Schema(example = "RUB") String currency,
        long offerCount
) {

    /** Преобразует строку агрегированного поиска в DTO списка каталога. */
    static ProductListResponse from(ProductCatalogProjection product) {
        return new ProductListResponse(product.getId(), product.getCatalogKey(), product.getBrand(), product.getName(),
                product.getCategoryCode(), product.getDescription(), product.getMinEffectivePrice(),
                product.getCurrency(), product.getOfferCount());
    }
}
