package com.pricehunter.query;

import com.pricehunter.offer.AvailabilityStatus;
import com.pricehunter.offer.ConditionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Готовые поля карточки предложения без загрузки графа JPA-сущностей. */
public interface OfferCardProjection {
    /** @return ID предложения */
    UUID getOfferId();
    /** @return ID городского рынка */
    UUID getMarketId();
    /** @return ID торговой сети */
    UUID getChainId();
    /** @return название торговой сети */
    String getChainName();
    /** @return ID города */
    UUID getCityId();
    /** @return название города */
    String getCityName();
    /** @return ID филиала или {@code null} для городской цены */
    UUID getStoreLocationId();
    /** @return название филиала */
    String getStoreLocationName();
    /** @return адрес филиала */
    String getStoreLocationAddress();
    /** @return ID общей модели товара */
    UUID getProductModelId();
    /** @return название общей модели */
    String getProductName();
    /** @return ID конфигурации */
    UUID getVariantId();
    /** @return название конфигурации */
    String getVariantName();
    /** @return состояние товара */
    ConditionType getConditionType();
    /** @return обычная цена */
    BigDecimal getRegularPrice();
    /** @return цена со скидкой */
    BigDecimal getSalePrice();
    /** @return цена при дополнительных условиях */
    BigDecimal getConditionalPrice();
    /** @return код валюты */
    String getCurrency();
    /** @return статус наличия */
    AvailabilityStatus getAvailabilityStatus();
    /** @return известное количество */
    Integer getQuantity();
    /** @return ссылка на товар */
    String getProductUrl();
    /** @return время последней проверки */
    Instant getLastCheckedAt();
}
