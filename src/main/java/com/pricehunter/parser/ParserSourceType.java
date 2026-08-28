package com.pricehunter.parser;

/** Внешний канал, из которого Price Hunter получает магазины или предложения. */
public enum ParserSourceType {
    YANDEX_SEARCH,
    GOOGLE_SEARCH,
    YANDEX_MAPS,
    TWO_GIS,
    WEBSITE
}
