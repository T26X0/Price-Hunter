package com.pricehunter.product;

import com.pricehunter.query.VariantIdentityProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.math.BigDecimal;

/** Запросы конфигураций товара и пакетное сопоставление канонических ключей. */
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    /** Находит точную конфигурацию внутри одной модели. */
    Optional<ProductVariant> findByProductModelIdAndCanonicalKey(UUID productModelId, String canonicalKey);

    /** Одним запросом возвращает уже существующие конфигурации из входного набора ключей. */
    @Query("""
            select v.id as id,
                   v.productModel.id as productModelId,
                   v.canonicalKey as canonicalKey,
                   v.displayName as displayName
              from ProductVariant v
             where v.productModel.id = :productModelId
               and v.canonicalKey in :canonicalKeys
            """)
    List<VariantIdentityProjection> findIdentities(
            @Param("productModelId") UUID productModelId,
            @Param("canonicalKeys") Collection<String> canonicalKeys);

    /**
     * Возвращает все варианты модели с агрегатами текущих предложений одним запросом.
     * Атрибуты загружаются вторым пакетным запросом через ProductVariantAttributeRepository.
     */
    @Query(value = """
            select v.id as \"id\",
                   v.canonical_key as \"canonicalKey\",
                   v.manufacturer_sku as \"manufacturerSku\",
                   v.display_name as \"displayName\",
                   min(coalesce(o.sale_price, o.regular_price, o.conditional_price)) as \"minEffectivePrice\",
                   min(o.currency) as \"currency\",
                   count(o.id) as \"offerCount\",
                   coalesce(bool_or(o.availability_status in ('IN_STOCK', 'PREORDER')), false) as \"available\"
              from product_variants v
              left join offers o on o.product_variant_id = v.id
                   and o.active = true
                   and (:cityId is null or exists (
                       select 1 from chain_city_markets market
                        where market.id = o.market_id and market.city_id = :cityId
                   ))
             where v.product_model_id = :productId
             group by v.id, v.canonical_key, v.manufacturer_sku, v.display_name
             order by v.display_name, v.id
            """, nativeQuery = true)
    List<ProductVariantCatalogProjection> findCatalogVariants(
            @Param("productId") UUID productId,
            @Param("cityId") UUID cityId);
}
