package com.pricehunter.offer;

import java.util.UUID;

public record ShippingQuoteUpsertResult(UUID shippingQuoteId, boolean created, boolean changed) {
}
