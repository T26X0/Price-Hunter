package com.pricehunter.offer;

import java.util.UUID;

public record OfferUpsertResult(UUID offerId, boolean created, boolean stateChanged) {
}
