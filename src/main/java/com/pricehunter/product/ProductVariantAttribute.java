package com.pricehunter.product;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "product_variant_attributes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductVariantAttribute {

    @EmbeddedId
    private ProductVariantAttributeId id;

    @MapsId("productVariantId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @MapsId("attributeDefinitionId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attribute_definition_id", nullable = false)
    private AttributeDefinition attributeDefinition;

    @Column(name = "normalized_value", nullable = false, length = 300)
    private String normalizedValue;

    @Column(name = "display_value", nullable = false, length = 300)
    private String displayValue;

    @Column(name = "numeric_value", precision = 19, scale = 4)
    private BigDecimal numericValue;

    @Column(name = "boolean_value")
    private Boolean booleanValue;

    public ProductVariantAttribute(ProductVariant productVariant, AttributeDefinition attributeDefinition,
                                   String normalizedValue, String displayValue,
                                   BigDecimal numericValue, Boolean booleanValue) {
        this.productVariant = productVariant;
        this.attributeDefinition = attributeDefinition;
        this.normalizedValue = normalizedValue.trim();
        this.displayValue = displayValue.trim();
        this.numericValue = numericValue;
        this.booleanValue = booleanValue;
    }
}
