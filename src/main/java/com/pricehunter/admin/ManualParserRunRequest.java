package com.pricehunter.admin;

import com.pricehunter.parser.ParserJobType;
import jakarta.validation.constraints.NotNull;

/** Тип безопасного ручного запуска настроенного источника. */
public record ManualParserRunRequest(@NotNull ParserJobType type) {
}
