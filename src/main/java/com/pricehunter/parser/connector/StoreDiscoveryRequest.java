package com.pricehunter.parser.connector;

import java.time.Instant;
import java.util.UUID;

/** Параметры поиска магазинов в одном городе по одной товарной категории. */
public record StoreDiscoveryRequest(
        UUID parserSourceId,
        UUID cityId,
        String cityName,
        String categoryQuery,
        Instant observedAt
) {
}
