package com.pricehunter.query;

import com.pricehunter.offer.AvailabilityStatus;

import java.math.BigDecimal;
import java.time.Instant;

public interface PriceHistoryPointProjection {
    Long getId();
    BigDecimal getRegularPrice();
    BigDecimal getSalePrice();
    BigDecimal getConditionalPrice();
    String getCurrency();
    AvailabilityStatus getAvailabilityStatus();
    Integer getQuantity();
    Instant getValidFrom();
    Instant getValidTo();
}
