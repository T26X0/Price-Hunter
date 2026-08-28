package com.pricehunter.offer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Полное наблюдаемое состояние предложения для идемпотентного обновления. */
public record OfferSnapshot(
        UUID marketId,
        UUID storeLocationId,
        UUID productVariantId,
        String externalOfferId,
        String offerKey,
        ConditionType conditionType,
        BigDecimal regularPrice,
        BigDecimal salePrice,
        BigDecimal conditionalPrice,
        String currency,
        AvailabilityStatus availabilityStatus,
        Integer quantity,
        String productUrl,
        Instant observedAt,
        Instant freshUntil,
        List<Map<String, Object>> terms
) {
    /** Проверяет идентичность, состояние, цены и количество до начала транзакции импорта. */
    public OfferSnapshot {
        if (marketId == null || productVariantId == null || offerKey == null || offerKey.isBlank()
                || conditionType == null || currency == null || currency.isBlank()
                || availabilityStatus == null || productUrl == null || productUrl.isBlank()
                || observedAt == null) {
            throw new IllegalArgumentException("Offer snapshot is missing required identity or state fields");
        }
        if (regularPrice == null && salePrice == null && conditionalPrice == null
                && availabilityStatus != AvailabilityStatus.OUT_OF_STOCK
                && availabilityStatus != AvailabilityStatus.UNKNOWN) {
            throw new IllegalArgumentException("An available offer must contain at least one price");
        }
        if (quantity != null && quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        terms = terms == null ? List.of() : List.copyOf(terms);
    }
}
