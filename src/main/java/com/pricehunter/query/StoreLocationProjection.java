package com.pricehunter.query;

import java.math.BigDecimal;
import java.util.UUID;

public interface StoreLocationProjection {
    UUID getId();
    String getExternalStoreId();
    String getName();
    String getAddress();
    BigDecimal getLatitude();
    BigDecimal getLongitude();
}
