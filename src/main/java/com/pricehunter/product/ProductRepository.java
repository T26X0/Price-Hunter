package com.pricehunter.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;
import java.math.BigDecimal;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

/** Запросы канонических моделей и облегчённых представлений каталога. */
public interface ProductRepository extends JpaRepository<Product, UUID> {
    /** Проверяет занятость ключа каталога без загрузки товара. */
    boolean existsBySkuIgnoreCase(String sku);

    /** Находит модель по ключу каталога без учёта регистра. */
    Optional<Product> findBySkuIgnoreCase(String catalogKey);

    /** Возвращает только ID модели для дешёвого сопоставления. */
    @Query("select p.id from Product p where lower(p.sku) = lower(:catalogKey)")
    Optional<UUID> findIdByCatalogKey(@Param("catalogKey") String catalogKey);

    /** Возвращает весь простой каталог узкой проекцией вместо тяжёлых сущностей. */
    @Query("""
            select p.id as id,
                   p.name as name,
                   p.sku as sku,
                   p.description as description,
                   p.createdAt as createdAt
              from Product p
             order by p.name, p.id
            """)
    java.util.List<ProductSummaryProjection> findAllSummaries();

    /**
     * Ищет модели для каталога одной агрегированной SQL-выборкой.
     * Предложения ограничиваются городом в JOIN, чтобы не создавать N+1 для цен карточек.
     */
    @Query(value = """
            select p.id as \"id\",
                   p.catalog_key as \"catalogKey\",
                   p.brand as \"brand\",
                   p.name as \"name\",
                   p.category_code as \"categoryCode\",
                   p.description as \"description\",
                   min(coalesce(o.sale_price, o.regular_price, o.conditional_price)) as \"minEffectivePrice\",
                   min(o.currency) as \"currency\",
                   count(o.id) as \"offerCount\"
              from product_models p
              left join product_variants v on v.product_model_id = p.id
              left join offers o on o.product_variant_id = v.id
                   and o.active = true
                   and (:cityId is null or exists (
                       select 1 from chain_city_markets market
                        where market.id = o.market_id and market.city_id = :cityId
                   ))
             where (:query is null or lower(p.name) like lower(concat('%', :query, '%'))
                    or lower(p.catalog_key) like lower(concat('%', :query, '%')))
               and (:brand is null or lower(p.brand) = lower(:brand))
               and (:category is null or lower(p.category_code) = lower(:category))
             group by p.id, p.catalog_key, p.brand, p.name, p.category_code, p.description
             order by p.name, p.id
            """,
            nativeQuery = true)
    Slice<ProductCatalogProjection> searchCatalog(
            @Param("query") String query,
            @Param("cityId") UUID cityId,
            @Param("category") String category,
            @Param("brand") String brand,
            Pageable pageable);

    /** Возвращает минимальную цену и число активных предложений модели в заданном городе или во всех городах. */
    @Query(value = """
            select min(coalesce(o.sale_price, o.regular_price, o.conditional_price)) as \"minEffectivePrice\",
                   min(o.currency) as \"currency\",
                   count(o.id) as \"offerCount\"
              from product_variants v
              left join offers o on o.product_variant_id = v.id
                   and o.active = true
                   and (:cityId is null or exists (
                       select 1 from chain_city_markets market
                        where market.id = o.market_id and market.city_id = :cityId
                   ))
             where v.product_model_id = :productId
            """, nativeQuery = true)
    ProductDetailStatsProjection findCatalogStats(
            @Param("productId") UUID productId,
            @Param("cityId") UUID cityId);
}
