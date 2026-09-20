package com.pricehunter.product;

import java.math.BigDecimal;

/** Агрегаты текущих предложений одной модели без загрузки вариантов и offers в память. */
public interface ProductDetailStatsProjection {
    BigDecimal getMinEffectivePrice();
    String getCurrency();
    long getOfferCount();
}
