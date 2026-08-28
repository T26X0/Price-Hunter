package com.pricehunter.parser.connector;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Map;

/**
 * Сырая карточка магазина из внешнего источника до поиска дублей и создания {@code Store}.
 * {@code rawPayload} сохраняет исходные данные для аудита и ручной проверки.
 */
public record DiscoveredStore(
        String externalId,
        String name,
        String address,
        String phone,
        BigDecimal latitude,
        BigDecimal longitude,
        URI websiteUri,
        URI sourceUri,
        Map<String, Object> rawPayload
) {
}
