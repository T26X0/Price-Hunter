package com.pricehunter.city;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** Публичный DTO города для фильтров каталога и выбора доставки. */
@Schema(description = "Город, в котором доступен каталог")
public record CityResponse(
        @Schema(example = "a1e554d7-79ca-49e5-b729-7f8323c480d5") UUID id,
        @Schema(example = "Екатеринбург") String name,
        @Schema(example = "RU") String countryCode,
        @Schema(example = "Asia/Yekaterinburg") String timezone
) {

    /** Преобразует внутреннюю сущность справочника в API-ответ. */
    public static CityResponse from(City city) {
        return new CityResponse(city.getId(), city.getName(), city.getCountryCode(), city.getTimezone());
    }
}
