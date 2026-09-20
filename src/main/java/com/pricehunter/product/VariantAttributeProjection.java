package com.pricehunter.product;

import java.math.BigDecimal;
import java.util.UUID;

/** Одна характеристика конфигурации, выбираемая для всех вариантов модели одним запросом. */
public interface VariantAttributeProjection {
    UUID getVariantId();
    String getCode();
    String getName();
    AttributeDataType getDataType();
    String getUnit();
    String getNormalizedValue();
    String getDisplayValue();
    BigDecimal getNumericValue();
    Boolean getBooleanValue();
}
