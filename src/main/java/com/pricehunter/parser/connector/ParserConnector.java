package com.pricehunter.parser.connector;

import com.pricehunter.parser.ConnectorMode;
import com.pricehunter.parser.ParserSourceType;

/**
 * Общий контракт любого адаптера внешнего источника.
 * Тройка «тип источника + режим + ключ» однозначно выбирает реализацию в реестре.
 */
public interface ParserConnector {
    /** @return канал данных, для которого предназначен адаптер */
    ParserSourceType sourceType();

    /** @return технический способ чтения источника */
    ConnectorMode mode();

    /** @return стабильный ключ реализации, например {@code 941} */
    String connectorKey();
}
