package com.pricehunter.offer;

import java.time.Instant;
import java.util.UUID;

/** Входной снимок наличия одной конфигурации в одном филиале. */
public record InventorySnapshot(
        UUID offerId,
        UUID storeLocationId,
        AvailabilityStatus availabilityStatus,
        Integer quantity,
        Instant observedAt
) {
    /** Проверяет обязательные идентификаторы и запрещает отрицательное количество. */
    public InventorySnapshot {
        if (offerId == null || storeLocationId == null || availabilityStatus == null || observedAt == null) {
            throw new IllegalArgumentException("Inventory snapshot is missing required fields");
        }
        if (quantity != null && quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
    }
}
