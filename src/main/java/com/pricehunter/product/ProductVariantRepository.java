package com.pricehunter.product;

import com.pricehunter.query.VariantIdentityProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
}
