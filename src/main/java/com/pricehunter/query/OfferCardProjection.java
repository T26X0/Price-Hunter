package com.pricehunter.query;

import com.pricehunter.offer.AvailabilityStatus;
import com.pricehunter.offer.ConditionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface OfferCardProjection {
    UUID getOfferId();
    UUID getMarketId();
    UUID getChainId();
    String getChainName();
    UUID getCityId();
    String getCityName();
    UUID getProductModelId();
    String getProductName();
    UUID getVariantId();
    String getVariantName();
    ConditionType getConditionType();
    BigDecimal getRegularPrice();
    BigDecimal getSalePrice();
    BigDecimal getConditionalPrice();
    String getCurrency();
    AvailabilityStatus getAvailabilityStatus();
    Integer getQuantity();
    String getProductUrl();
    Instant getLastCheckedAt();
}
