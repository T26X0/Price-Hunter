package com.pricehunter.product;

import java.time.Instant;
import java.util.UUID;

public interface ProductSummaryProjection {
    UUID getId();
    String getName();
    String getSku();
    String getDescription();
    Instant getCreatedAt();
}
