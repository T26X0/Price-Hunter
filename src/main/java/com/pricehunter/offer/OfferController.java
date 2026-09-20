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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Открывает текущие предложения, их условия, историю цены и остатки для frontend. */
@RestController
@RequestMapping(ApiRoutes.Offers.ROOT)
@RequiredArgsConstructor
@Validated
@Tag(name = "Offers", description = "Предложения магазинов, история цен и остатки")
public class OfferController {

    private final OfferQueryService offerQueryService;

    /** Возвращает коллекцию предложений; variantId и cityId обязательны для однозначной выборки. */
    @GetMapping
    @Operation(summary = "Предложения варианта в городе")
    public SliceResponse<OfferResponse> findAll(
            @Parameter(required = true) @RequestParam UUID variantId,
            @Parameter(required = true) @RequestParam UUID cityId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return offerQueryService.findVariantOffers(variantId, cityId, page, size);
    }

    /** Возвращает одну полную карточку предложения вместе с действующими условиями покупки. */
    @GetMapping(ApiRoutes.Offers.BY_ID)
    @Operation(summary = "Карточка предложения")
    public OfferResponse findById(@PathVariable UUID offerId) {
        return offerQueryService.findOffer(offerId);
    }

    /** Возвращает интервалы изменений цены и наличия, пригодные для графика без дополнительной обработки. */
    @GetMapping(ApiRoutes.Offers.HISTORY)
    @Operation(summary = "История цены и наличия предложения")
    public List<PriceHistoryResponse> findHistory(
            @PathVariable UUID offerId,
            @Parameter(description = "Начало диапазона в ISO-8601") @RequestParam(required = false) Instant from,
            @Parameter(description = "Конец диапазона в ISO-8601") @RequestParam(required = false) Instant to) {
        return offerQueryService.findHistory(offerId, from, to);
    }

    /** Возвращает наличие по филиалам, если сеть передала раздельные остатки. */
    @GetMapping(ApiRoutes.Offers.INVENTORY)
    @Operation(summary = "Наличие предложения по филиалам")
    public SliceResponse<InventoryResponse> findInventory(
            @PathVariable UUID offerId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return offerQueryService.findInventory(offerId, page, size);
    }
}
