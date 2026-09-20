package com.pricehunter.product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Полная публичная карточка общей модели, независимой от города и магазина. */
@Schema(description = "Карточка модели товара со всеми доступными вариантами")
public record ProductDetailResponse(
        UUID id,
        @Schema(example = "APPLE-IPHONE-16-PRO") String catalogKey,
        @Schema(example = "Apple") String brand,
        @Schema(example = "iPhone 16 Pro") String name,
        @Schema(example = "smartphones") String category,
        String description,
        BigDecimal minEffectivePrice,
        @Schema(example = "RUB") String currency,
        long offerCount,
        List<ProductVariantResponse> variants
) {
}
