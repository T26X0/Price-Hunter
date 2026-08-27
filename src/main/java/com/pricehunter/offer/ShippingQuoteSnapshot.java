package com.pricehunter.offer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ShippingQuoteSnapshot(
        UUID offerId,
        UUID destinationCityId,
        boolean available,
        BigDecimal deliveryPrice,
        String currency,
        Short minDeliveryDays,
        Short maxDeliveryDays,
        Instant observedAt
) {
    public ShippingQuoteSnapshot {
        if (offerId == null || destinationCityId == null || currency == null || currency.isBlank()
                || observedAt == null) {
            throw new IllegalArgumentException("Shipping quote is missing required fields");
        }
        if (deliveryPrice != null && deliveryPrice.signum() < 0) {
            throw new IllegalArgumentException("Delivery price cannot be negative");
        }
        if (minDeliveryDays != null && minDeliveryDays < 0
                || maxDeliveryDays != null && maxDeliveryDays < 0
                || minDeliveryDays != null && maxDeliveryDays != null
                && minDeliveryDays > maxDeliveryDays) {
            throw new IllegalArgumentException("Delivery day range is invalid");
        }
    }
}
