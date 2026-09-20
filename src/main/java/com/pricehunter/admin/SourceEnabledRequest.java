package com.pricehunter.admin;

import jakarta.validation.constraints.NotNull;

/** Явное изменение доступности сохранённого источника для плановых и ручных запусков. */
public record SourceEnabledRequest(@NotNull Boolean enabled) {
}
