package com.pricehunter.query;

import java.time.Instant;
import java.util.UUID;

public interface OfferIdentityProjection {
    UUID getId();
    String getExternalOfferId();
    String getOfferKey();
    String getStateHash();
    Instant getLastCheckedAt();
}
