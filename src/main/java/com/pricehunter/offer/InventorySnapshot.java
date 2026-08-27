package com.pricehunter.offer;

import java.time.Instant;
import java.util.UUID;

public record InventorySnapshot(
        UUID offerId,
        UUID storeLocationId,
        AvailabilityStatus availabilityStatus,
        Integer quantity,
        Instant observedAt
) {
    public InventorySnapshot {
        if (offerId == null || storeLocationId == null || availabilityStatus == null || observedAt == null) {
            throw new IllegalArgumentException("Inventory snapshot is missing required fields");
        }
        if (quantity != null && quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
    }
}
