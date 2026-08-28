package com.pricehunter.parser.connector;

import com.pricehunter.offer.AvailabilityStatus;
import com.pricehunter.offer.ConditionType;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Унифицированная товарная позиция, которую вернул конкретный адаптер сайта.
 * Это ещё не канонический товар: модель и конфигурация определяются следующим этапом нормализации.
 */
public record ParsedCatalogItem(
        String externalId,
        String externalStoreId,
        URI sourceUri,
        String rawName,
        String brand,
        String sourceCategory,
        ConditionType conditionType,
        BigDecimal regularPrice,
        BigDecimal salePrice,
        BigDecimal conditionalPrice,
        String currency,
        AvailabilityStatus availabilityStatus,
        Integer quantity,
        Map<String, String> attributes,
        List<Map<String, Object>> terms,
        Map<String, Object> rawPayload
) {
    /** Проверяет минимальный набор данных и создаёт неизменяемые коллекции атрибутов и условий. */
    public ParsedCatalogItem {
        if (externalId == null || externalId.isBlank() || sourceUri == null
                || rawName == null || rawName.isBlank() || currency == null || currency.isBlank()
                || availabilityStatus == null || conditionType == null) {
            throw new IllegalArgumentException("Parsed catalog item is missing required fields");
        }
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        terms = terms == null ? List.of() : List.copyOf(terms);
        rawPayload = rawPayload == null ? Map.of() : Map.copyOf(rawPayload);
    }
}
