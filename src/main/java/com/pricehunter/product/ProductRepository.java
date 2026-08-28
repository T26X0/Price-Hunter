package com.pricehunter.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

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
}
