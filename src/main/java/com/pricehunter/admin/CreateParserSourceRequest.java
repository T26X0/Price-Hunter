package com.pricehunter.admin;

import com.pricehunter.parser.ConnectorMode;
import com.pricehunter.parser.ParserSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

/** Проверяемые данные источника парсинга, которые сохраняются перед любым сетевым запуском. */
public record CreateParserSourceRequest(
        @NotNull UUID marketId,
        @NotNull ParserSourceType sourceType,
        @NotNull ConnectorMode connectorMode,
        @NotBlank @Size(max = 100) String connectorKey,
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 1000) String baseUrl,
        Map<String, Object> configuration,
        Boolean enabled
) {
}
