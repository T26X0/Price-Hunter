package com.pricehunter.query;

import java.util.UUID;

/** Минимальные поля конфигурации для пакетного поиска дублей. */
public interface VariantIdentityProjection {
    /** @return ID конфигурации */
    UUID getId();
    /** @return ID общей модели */
    UUID getProductModelId();
    /** @return канонический ключ характеристик */
    String getCanonicalKey();
    /** @return отображаемое название конфигурации */
    String getDisplayName();
}
