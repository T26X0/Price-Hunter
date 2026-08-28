package com.pricehunter.query;

import com.pricehunter.offer.AvailabilityStatus;

import java.time.Instant;
import java.util.UUID;

/** Компактное представление текущего наличия конфигурации в филиале. */
public interface InventoryProjection {
    /** @return ID филиала */
    UUID getStoreLocationId();
    /** @return название филиала */
    String getStoreName();
    /** @return адрес филиала */
    String getAddress();
    /** @return статус наличия */
    AvailabilityStatus getAvailabilityStatus();
    /** @return известное количество товара */
    Integer getQuantity();
    /** @return время последней проверки */
    Instant getLastCheckedAt();
}
