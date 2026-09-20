package com.pricehunter.admin;

import com.pricehunter.retail.SalesChannel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Проверяемые данные для добавления рынка сети в конкретном городе. */
public record CreateMarketRequest(
        @NotNull UUID chainId,
        @NotNull UUID cityId,
        @NotNull SalesChannel salesChannel,
        @Size(max = 200) String externalMarketId,
        @Size(max = 1000) String sourceBaseUrl,
        Boolean parserEnabled
) {
}
