package com.pricehunter.product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/** Публичное фильтруемое свойство варианта: память, цвет, SIM и другие характеристики. */
@Schema(description = "Характеристика варианта товара")
public record VariantAttributeResponse(
        @Schema(example = "storage") String code,
        @Schema(example = "Объём памяти") String name,
        AttributeDataType dataType,
        @Schema(example = "GB") String unit,
        @Schema(example = "256 GB") String value,
        @Schema(example = "256") BigDecimal numericValue,
        Boolean booleanValue
) {

    /** Преобразует пакетную проекцию характеристики в DTO. */
    static VariantAttributeResponse from(VariantAttributeProjection attribute) {
        return new VariantAttributeResponse(attribute.getCode(), attribute.getName(), attribute.getDataType(),
                attribute.getUnit(), attribute.getDisplayValue(), attribute.getNumericValue(),
                attribute.getBooleanValue());
    }
}
