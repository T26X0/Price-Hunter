package com.pricehunter.query;

import java.util.UUID;

public interface VariantIdentityProjection {
    UUID getId();
    UUID getProductModelId();
    String getCanonicalKey();
    String getDisplayName();
}
