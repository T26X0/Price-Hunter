package com.pricehunter.offer;

import com.pricehunter.query.InventoryProjection;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/** Текущее наличие одного предложения в конкретном филиале. */
@Schema(description = "Наличие предложения в филиале")
public record InventoryResponse(
        UUID storeId,
        String storeName,
        String address,
        AvailabilityStatus availability,
        Integer quantity,
        Instant lastCheckedAt
) {

    /** Преобразует проекцию остатков в публичный DTO. */
    static InventoryResponse from(InventoryProjection inventory) {
        return new InventoryResponse(inventory.getStoreLocationId(), inventory.getStoreName(), inventory.getAddress(),
                inventory.getAvailabilityStatus(), inventory.getQuantity(), inventory.getLastCheckedAt());
    }
}
