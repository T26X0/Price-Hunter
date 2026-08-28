package com.pricehunter.parser.identity;

import com.pricehunter.parser.connector.ParsedCatalogItem;

/** Определяет каноническую модель и конфигурацию товара либо отправляет позицию на проверку. */
public interface ProductIdentityNormalizer {
    /**
     * @param item сырая позиция внешнего каталога
     * @return нормализованный кандидат с решением об автоматическом импорте
     */
    NormalizedProductCandidate normalize(ParsedCatalogItem item);
}
