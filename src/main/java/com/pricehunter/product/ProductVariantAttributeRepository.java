package com.pricehunter.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.UUID;
import java.util.List;

/** Доступ к значениям характеристик конкретных конфигураций. */
public interface ProductVariantAttributeRepository
        extends JpaRepository<ProductVariantAttribute, ProductVariantAttributeId> {

    /** Удаляет выбранные атрибуты конфигурации одним SQL-запросом. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ProductVariantAttribute a where a.productVariant.id = :variantId " +
           "and a.attributeDefinition.id in :attributeIds")
    int deleteVariantAttributes(
            @Param("variantId") UUID variantId,
            @Param("attributeIds") Collection<UUID> attributeIds);

    /** Загружает характеристики всех вариантов модели за один запрос для публичной карточки. */
    @Query("""
            select attribute.productVariant.id as variantId,
                   definition.code as code,
                   definition.name as name,
                   definition.dataType as dataType,
                   definition.unit as unit,
                   attribute.normalizedValue as normalizedValue,
                   attribute.displayValue as displayValue,
                   attribute.numericValue as numericValue,
                   attribute.booleanValue as booleanValue
              from ProductVariantAttribute attribute
              join attribute.attributeDefinition definition
             where attribute.productVariant.productModel.id = :productId
             order by attribute.productVariant.displayName, definition.code
            """)
    List<VariantAttributeProjection> findCatalogAttributes(@Param("productId") UUID productId);
}
