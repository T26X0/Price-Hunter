package com.pricehunter.parser.connector;

import com.pricehunter.parser.ConnectorMode;
import com.pricehunter.parser.ParserSourceType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
/** Реестр адаптеров, выбирающий реализацию без условных операторов в бизнес-логике импорта. */
public class ConnectorRegistry {

    private final Map<ConnectorKey, ParserConnector> connectors;

    /** Индексирует все Spring-компоненты и сразу останавливает запуск при конфликте ключей. */
    public ConnectorRegistry(List<ParserConnector> connectors) {
        Map<ConnectorKey, ParserConnector> indexed = new HashMap<>();
        for (ParserConnector connector : connectors) {
            ConnectorKey key = ConnectorKey.of(connector.sourceType(), connector.mode(), connector.connectorKey());
            ParserConnector duplicate = indexed.putIfAbsent(key, connector);
            if (duplicate != null) {
                throw new IllegalStateException("Duplicate parser connector: " + key);
            }
        }
        this.connectors = Map.copyOf(indexed);
    }

    /** Возвращает адаптер каталога для указанной комбинации источника, режима и ключа. */
    public CatalogConnector catalog(ParserSourceType sourceType, ConnectorMode mode, String connectorKey) {
        ParserConnector connector = required(sourceType, mode, connectorKey);
        if (connector instanceof CatalogConnector catalogConnector) {
            return catalogConnector;
        }
        throw new IllegalStateException("Configured connector does not support catalog scans: " + connectorKey);
    }

    /** Возвращает адаптер обнаружения магазинов для указанной комбинации. */
    public StoreDiscoveryConnector storeDiscovery(
            ParserSourceType sourceType, ConnectorMode mode, String connectorKey) {
        ParserConnector connector = required(sourceType, mode, connectorKey);
        if (connector instanceof StoreDiscoveryConnector storeConnector) {
            return storeConnector;
        }
        throw new IllegalStateException("Configured connector does not support store discovery: " + connectorKey);
    }

    /** Ищет обязательный адаптер и выдаёт понятную ошибку конфигурации при его отсутствии. */
    private ParserConnector required(ParserSourceType sourceType, ConnectorMode mode, String connectorKey) {
        ConnectorKey key = ConnectorKey.of(sourceType, mode, connectorKey);
        ParserConnector connector = connectors.get(key);
        if (connector == null) {
            throw new IllegalStateException("No parser connector registered for " + key);
        }
        return connector;
    }

    /** Нормализованный составной ключ реестра. */
    private record ConnectorKey(ParserSourceType sourceType, ConnectorMode mode, String connectorKey) {
        /** Создаёт ключ с регистронезависимым именем реализации. */
        private static ConnectorKey of(ParserSourceType sourceType, ConnectorMode mode, String connectorKey) {
            return new ConnectorKey(sourceType, mode, connectorKey.trim().toLowerCase(Locale.ROOT));
        }
    }
}
