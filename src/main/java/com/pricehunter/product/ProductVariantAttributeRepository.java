package com.pricehunter.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.UUID;

public interface ProductVariantAttributeRepository
        extends JpaRepository<ProductVariantAttribute, ProductVariantAttributeId> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ProductVariantAttribute a where a.productVariant.id = :variantId " +
           "and a.attributeDefinition.id in :attributeIds")
    int deleteVariantAttributes(
            @Param("variantId") UUID variantId,
            @Param("attributeIds") Collection<UUID> attributeIds);
}
