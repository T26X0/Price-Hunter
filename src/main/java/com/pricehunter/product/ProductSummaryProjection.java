package com.pricehunter.product;

import java.time.Instant;
import java.util.UUID;

/** Узкое представление модели для списка товаров без загрузки JPA-сущности. */
public interface ProductSummaryProjection {
    /** @return ID модели */
    UUID getId();
    /** @return название модели */
    String getName();
    /** @return ключ каталога */
    String getSku();
    /** @return описание модели */
    String getDescription();
    /** @return дата создания */
    Instant getCreatedAt();
}
