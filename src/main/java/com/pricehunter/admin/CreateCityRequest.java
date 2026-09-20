package com.pricehunter.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Проверяемые данные для добавления города в административном контуре. */
public record CreateCityRequest(
        @NotBlank @Size(max = 200) String name,
        @Pattern(regexp = "[A-Za-z]{2}") String countryCode,
        @Size(max = 100) String timezone
) {
}
