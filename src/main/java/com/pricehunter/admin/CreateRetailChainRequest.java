package com.pricehunter.admin;

import com.pricehunter.store.ParserType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Проверяемые данные для добавления торговой сети. */
public record CreateRetailChainRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 100) String code,
        @Size(max = 1000) String websiteUrl,
        ParserType parserType,
        Boolean parserEnabled
) {
}
