package com.pricehunter.parser;

/**
 * Способ технического подключения к источнику данных.
 * Позволяет менять HTML-парсинг на браузер или API, не меняя остальной конвейер импорта.
 */
public enum ConnectorMode {
    HTML,
    BROWSER,
    API
}
