package com.pricehunter.offer;

import java.util.UUID;

/** Результат идемпотентного обновления стоимости доставки. */
public record ShippingQuoteUpsertResult(UUID shippingQuoteId, boolean created, boolean changed) {
}
