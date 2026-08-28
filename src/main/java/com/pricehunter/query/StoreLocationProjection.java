package com.pricehunter.query;

import java.math.BigDecimal;
import java.util.UUID;

/** Координаты и адрес активного филиала для парсера или интерфейса. */
public interface StoreLocationProjection {
    /** @return внутренний ID филиала */
    UUID getId();
    /** @return ID филиала во внешнем источнике */
    String getExternalStoreId();
    /** @return название филиала */
    String getName();
    /** @return адрес филиала */
    String getAddress();
    /** @return широта */
    BigDecimal getLatitude();
    /** @return долгота */
    BigDecimal getLongitude();
}
