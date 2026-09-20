package com.pricehunter.product;

import java.math.BigDecimal;
import java.util.UUID;

/** Узкая агрегированная строка списка моделей для карточек каталога. */
public interface ProductCatalogProjection {
    UUID getId();
    String getCatalogKey();
    String getBrand();
    String getName();
    String getCategoryCode();
    String getDescription();
    BigDecimal getMinEffectivePrice();
    String getCurrency();
    long getOfferCount();
}
