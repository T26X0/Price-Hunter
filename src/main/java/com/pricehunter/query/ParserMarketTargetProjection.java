package com.pricehunter.query;

import com.pricehunter.retail.SalesChannel;
import com.pricehunter.store.ParserType;

import java.util.UUID;

/** Минимальная конфигурация городского рынка, необходимая исполнителю парсинга. */
public interface ParserMarketTargetProjection {
    /** @return ID городского рынка */
    UUID getMarketId();
    /** @return ID торговой сети */
    UUID getChainId();
    /** @return стабильный код сети */
    String getChainCode();
    /** @return назначенный тип парсера */
    ParserType getParserType();
    /** @return ID города */
    UUID getCityId();
    /** @return название города */
    String getCityName();
    /** @return канал продаж */
    SalesChannel getSalesChannel();
    /** @return базовый адрес источника */
    String getSourceBaseUrl();
}
