package com.pricehunter.offer;

import com.pricehunter.query.PriceHistoryPointProjection;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/** Точка интервальной истории цены и наличия, сразу пригодная для графика frontend. */
@Schema(description = "Интервал цены и наличия на графике")
public record PriceHistoryResponse(
        long id,
        BigDecimal regularPrice,
        BigDecimal salePrice,
        BigDecimal conditionalPrice,
        BigDecimal effectivePrice,
        String currency,
        AvailabilityStatus availability,
        Integer quantity,
        Instant validFrom,
        Instant validTo
) {

    /** Преобразует проекцию истории в стабильный ответ графика. */
    static PriceHistoryResponse from(PriceHistoryPointProjection point) {
        BigDecimal effective = point.getSalePrice() != null ? point.getSalePrice()
                : point.getRegularPrice() != null ? point.getRegularPrice() : point.getConditionalPrice();
        return new PriceHistoryResponse(point.getId(), point.getRegularPrice(), point.getSalePrice(),
                point.getConditionalPrice(), effective, point.getCurrency(), point.getAvailabilityStatus(),
                point.getQuantity(), point.getValidFrom(), point.getValidTo());
    }
}
