package com.pricehunter.product;

import java.math.BigDecimal;
import java.util.UUID;

/** Агрегированная строка конфигурации модели для публичной карточки товара. */
public interface ProductVariantCatalogProjection {
    UUID getId();
    String getCanonicalKey();
    String getManufacturerSku();
    String getDisplayName();
    BigDecimal getMinEffectivePrice();
    String getCurrency();
    long getOfferCount();
    boolean getAvailable();
}
