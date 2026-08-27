package com.pricehunter.query;

import java.math.BigDecimal;
import java.util.UUID;

public interface DeliveredOfferProjection {
    UUID getOfferId();
    String getChainName();
    String getSourceCityName();
    BigDecimal getItemPrice();
    BigDecimal getDeliveryPrice();
    BigDecimal getTotalPrice();
    Short getMinDeliveryDays();
    Short getMaxDeliveryDays();
    String getCurrency();
    String getProductUrl();
}
