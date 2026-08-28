package com.pricehunter.parser.connector;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

/** Неизменяемые входные данные одного запуска парсера каталога. */
public record CatalogScanRequest(
        UUID parserSourceId,
        UUID marketId,
        URI catalogUri,
        String cityKey,
        Instant observedAt
) {
    /** Проверяет обязательные поля до сетевого запроса, чтобы не создавать некорректное задание. */
    public CatalogScanRequest {
        if (parserSourceId == null || marketId == null || catalogUri == null
                || cityKey == null || cityKey.isBlank() || observedAt == null) {
            throw new IllegalArgumentException("Catalog scan request is incomplete");
        }
    }
}
