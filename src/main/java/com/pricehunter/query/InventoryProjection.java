package com.pricehunter.query;

import com.pricehunter.offer.AvailabilityStatus;

import java.time.Instant;
import java.util.UUID;

public interface InventoryProjection {
    UUID getStoreLocationId();
    String getStoreName();
    String getAddress();
    AvailabilityStatus getAvailabilityStatus();
    Integer getQuantity();
    Instant getLastCheckedAt();
}
