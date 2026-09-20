package com.pricehunter.offer;

import com.pricehunter.query.DeliveredOfferProjection;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/** Предложение из другого города с рассчитанной итоговой ценой доставки. */
@Schema(description = "Предложение с доставкой в выбранный город")
public record DeliveredOfferResponse(
        UUID offerId,
        String chainName,
        String sourceCityName,
        BigDecimal itemPrice,
        BigDecimal deliveryPrice,
        BigDecimal totalPrice,
        Short minDeliveryDays,
        Short maxDeliveryDays,
        String currency,
        String productUrl
) {

    /** Преобразует готовый расчёт доставки в ответ API. */
    static DeliveredOfferResponse from(DeliveredOfferProjection offer) {
        return new DeliveredOfferResponse(offer.getOfferId(), offer.getChainName(), offer.getSourceCityName(),
                offer.getItemPrice(), offer.getDeliveryPrice(), offer.getTotalPrice(), offer.getMinDeliveryDays(),
                offer.getMaxDeliveryDays(), offer.getCurrency(), offer.getProductUrl());
    }
}
