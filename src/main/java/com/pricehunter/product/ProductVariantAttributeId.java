package com.pricehunter.product;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
public record ProductVariantAttributeId(
        @Column(name = "product_variant_id") UUID productVariantId,
        @Column(name = "attribute_definition_id") UUID attributeDefinitionId
) implements Serializable {
}
