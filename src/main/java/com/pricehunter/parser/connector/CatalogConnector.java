package com.pricehunter.parser.connector;

/** Адаптер, который читает каталог конкретного магазина и возвращает унифицированные позиции. */
public interface CatalogConnector extends ParserConnector {
    /**
     * Выполняет одно чтение каталога.
     *
     * @param request источник, рынок, город и момент наблюдения
     * @return снимок найденных позиций
     */
    CatalogScanResult scan(CatalogScanRequest request);
}
