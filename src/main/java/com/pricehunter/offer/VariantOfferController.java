package com.pricehunter.offer;

import com.pricehunter.shared.ApiRoutes;
import com.pricehunter.shared.SliceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Открывает сравнение магазинов и доставок внутри одной конфигурации товара. */
@RestController
@RequestMapping(ApiRoutes.Variants.ROOT)
@RequiredArgsConstructor
@Validated
@Tag(name = "Variants", description = "Сравнение предложений для конфигурации товара")
public class VariantOfferController {

    private final OfferQueryService offerQueryService;

    /** Возвращает городские предложения варианта, начиная с самой низкой текущей цены. */
    @GetMapping(ApiRoutes.Variants.OFFERS)
    @Operation(summary = "Предложения варианта в выбранном городе")
    public SliceResponse<OfferResponse> findOffers(
            @PathVariable UUID variantId,
            @Parameter(required = true) @RequestParam UUID cityId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return offerQueryService.findVariantOffers(variantId, cityId, page, size);
    }

    /** Возвращает предложения из других городов с заранее рассчитанной стоимостью доставки. */
    @GetMapping(ApiRoutes.Variants.DELIVERED_OFFERS)
    @Operation(summary = "Предложения варианта с доставкой")
    public SliceResponse<DeliveredOfferResponse> findDeliveredOffers(
            @PathVariable UUID variantId,
            @Parameter(required = true, description = "Город доставки") @RequestParam UUID cityId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return offerQueryService.findDeliveredOffers(variantId, cityId, page, size);
    }
}
