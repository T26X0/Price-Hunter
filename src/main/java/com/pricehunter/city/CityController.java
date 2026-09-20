package com.pricehunter.city;

import com.pricehunter.shared.ApiRoutes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Открывает справочник городов для фильтров и выбора города доставки. */
@RestController
@RequestMapping(ApiRoutes.Cities.ROOT)
@RequiredArgsConstructor
@Tag(name = "Cities", description = "Города, доступные в каталоге")
public class CityController {

    private final CityService cityService;

    /** Возвращает все города, в которых могут существовать предложения. */
    @GetMapping
    @Operation(summary = "Список городов")
    public List<CityResponse> findAll() {
        return cityService.findAll();
    }
}
