package com.pricehunter.offer;

import java.util.UUID;

/** Результат обновления предложения с признаками создания и изменения истории. */
public record OfferUpsertResult(UUID offerId, boolean created, boolean stateChanged) {
}
