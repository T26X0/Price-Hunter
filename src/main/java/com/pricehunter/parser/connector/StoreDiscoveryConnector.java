package com.pricehunter.parser.connector;

/** Адаптер поисковика или карт, который обнаруживает магазины в заданном городе. */
public interface StoreDiscoveryConnector extends ParserConnector {
    /**
     * Ищет магазины по категории и городу.
     *
     * @param request параметры поиска
     * @return найденные внешние карточки магазинов
     */
    StoreDiscoveryResult discover(StoreDiscoveryRequest request);
}
