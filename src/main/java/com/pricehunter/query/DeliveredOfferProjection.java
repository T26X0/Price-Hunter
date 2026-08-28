package com.pricehunter.query;

import java.math.BigDecimal;
import java.util.UUID;

/** Итоговая цена предложения с доставкой в выбранный город для межгородского сравнения. */
public interface DeliveredOfferProjection {
    /** @return ID предложения */
    UUID getOfferId();
    /** @return название торговой сети */
    String getChainName();
    /** @return город, из которого отправляется товар */
    String getSourceCityName();
    /** @return цена самого товара */
    BigDecimal getItemPrice();
    /** @return стоимость доставки */
    BigDecimal getDeliveryPrice();
    /** @return итоговая цена товара с доставкой */
    BigDecimal getTotalPrice();
    /** @return минимальный срок доставки в днях */
    Short getMinDeliveryDays();
    /** @return максимальный срок доставки в днях */
    Short getMaxDeliveryDays();
    /** @return код валюты */
    String getCurrency();
    /** @return ссылка на товар у продавца */
    String getProductUrl();
}
