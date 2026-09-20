package com.pricehunter.offer;

import com.pricehunter.query.OfferCardProjection;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Публичная карточка текущего предложения магазина для одной конфигурации товара. */
@Schema(description = "Актуальное предложение магазина")
public record OfferResponse(
        UUID id,
        UUID marketId,
        UUID chainId,
        String chainName,
        UUID cityId,
        String cityName,
        StoreResponse store,
        UUID productId,
        String productName,
        UUID variantId,
        String variantName,
        ConditionType condition,
        BigDecimal regularPrice,
        BigDecimal salePrice,
        BigDecimal conditionalPrice,
        BigDecimal effectivePrice,
        @Schema(example = "RUB") String currency,
        AvailabilityStatus availability,
        Integer quantity,
        String productUrl,
        Instant lastCheckedAt,
        List<OfferTermResponse> terms
) {

    /** Преобразует оптимизированную SQL-проекцию и уже прочитанные условия в публичный ответ. */
    static OfferResponse from(OfferCardProjection offer, List<OfferTermResponse> terms) {
        StoreResponse store = offer.getStoreLocationId() == null ? null
                : new StoreResponse(offer.getStoreLocationId(), offer.getStoreLocationName(),
                offer.getStoreLocationAddress());
        return new OfferResponse(offer.getOfferId(), offer.getMarketId(), offer.getChainId(), offer.getChainName(),
                offer.getCityId(), offer.getCityName(), store, offer.getProductModelId(), offer.getProductName(),
                offer.getVariantId(), offer.getVariantName(), offer.getConditionType(), offer.getRegularPrice(),
                offer.getSalePrice(), offer.getConditionalPrice(), effectivePrice(offer), offer.getCurrency(),
                offer.getAvailabilityStatus(), offer.getQuantity(), offer.getProductUrl(), offer.getLastCheckedAt(), terms);
    }

    /** Определяет отображаемую текущую цену, соблюдая существующий приоритет цены со скидкой. */
    private static BigDecimal effectivePrice(OfferCardProjection offer) {
        if (offer.getSalePrice() != null) {
            return offer.getSalePrice();
        }
        return offer.getRegularPrice() != null ? offer.getRegularPrice() : offer.getConditionalPrice();
    }
}
