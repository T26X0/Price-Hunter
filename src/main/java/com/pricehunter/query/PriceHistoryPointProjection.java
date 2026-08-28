package com.pricehunter.query;

import com.pricehunter.offer.AvailabilityStatus;

import java.math.BigDecimal;
import java.time.Instant;

/** Одна интервальная точка графика цены и наличия конкретного предложения. */
public interface PriceHistoryPointProjection {
    /** @return ID интервала истории */
    Long getId();
    /** @return обычная цена */
    BigDecimal getRegularPrice();
    /** @return цена со скидкой */
    BigDecimal getSalePrice();
    /** @return цена при дополнительных условиях */
    BigDecimal getConditionalPrice();
    /** @return код валюты */
    String getCurrency();
    /** @return наличие в течение интервала */
    AvailabilityStatus getAvailabilityStatus();
    /** @return количество в течение интервала */
    Integer getQuantity();
    /** @return начало интервала */
    Instant getValidFrom();
    /** @return конец интервала или {@code null} для текущего состояния */
    Instant getValidTo();
}
