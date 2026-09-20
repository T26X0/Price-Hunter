package com.pricehunter.product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Публичная конфигурация одной модели с характеристиками и актуальными агрегатами цен. */
@Schema(description = "Вариант товара с характеристиками и состоянием предложений")
public record ProductVariantResponse(
        UUID id,
        @Schema(example = "storage=256 gb;color=black") String canonicalKey,
        @Schema(example = "256 ГБ, Black") String displayName,
        String manufacturerSku,
        List<VariantAttributeResponse> attributes,
        BigDecimal minEffectivePrice,
        @Schema(example = "RUB") String currency,
        long offerCount,
        boolean available
) {

    /** Собирает DTO варианта из агрегированной строки и уже загруженных характеристик. */
    static ProductVariantResponse from(ProductVariantCatalogProjection variant,
                                       List<VariantAttributeResponse> attributes) {
        return new ProductVariantResponse(variant.getId(), variant.getCanonicalKey(), variant.getDisplayName(),
                variant.getManufacturerSku(), attributes, variant.getMinEffectivePrice(), variant.getCurrency(),
                variant.getOfferCount(), variant.getAvailable());
    }
}
