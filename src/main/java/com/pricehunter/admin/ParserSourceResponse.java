package com.pricehunter.admin;

import com.pricehunter.parser.ConnectorMode;
import com.pricehunter.parser.ParserSource;
import com.pricehunter.parser.ParserSourceType;

import java.time.Instant;
import java.util.UUID;

/** Публичное административное представление настроенного источника. */
public record ParserSourceResponse(
        UUID id,
        UUID marketId,
        UUID cityId,
        String cityName,
        UUID chainId,
        String chainName,
        ParserSourceType sourceType,
        ConnectorMode connectorMode,
        String connectorKey,
        String name,
        String baseUrl,
        boolean enabled,
        Instant lastPriceScanAt,
        Instant lastProductScanAt,
        Instant lastStoreScanAt
) {
    /** Преобразует источник с заранее загруженными рынком, городом и сетью в DTO. */
    static ParserSourceResponse from(ParserSource source) {
        var market = source.getMarket();
        return new ParserSourceResponse(source.getId(), market == null ? null : market.getId(),
                market == null ? null : market.getCity().getId(), market == null ? null : market.getCity().getName(),
                market == null ? null : market.getRetailChain().getId(),
                market == null ? null : market.getRetailChain().getName(), source.getSourceType(), source.getConnectorMode(),
                source.getConnectorKey(), source.getName(), source.getBaseUrl(), source.isEnabled(),
                source.getLastPriceScanAt(), source.getLastProductScanAt(), source.getLastStoreScanAt());
    }
}
