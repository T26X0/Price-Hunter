package com.pricehunter.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    boolean existsBySkuIgnoreCase(String sku);

    Optional<Product> findBySkuIgnoreCase(String catalogKey);

    @Query("select p.id from Product p where lower(p.sku) = lower(:catalogKey)")
    Optional<UUID> findIdByCatalogKey(@Param("catalogKey") String catalogKey);

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
