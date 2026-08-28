package com.pricehunter.parser.connector;

import java.net.URI;
import java.time.Instant;
import java.util.List;

/** Результат чтения одной страницы или одной API-выдачи каталога. */
public record CatalogScanResult(URI sourceUri, Instant fetchedAt, List<ParsedCatalogItem> items) {
    /** Делает список позиций неизменяемым и заменяет отсутствующий список пустым. */
    public CatalogScanResult {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
