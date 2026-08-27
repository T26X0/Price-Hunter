package com.pricehunter.store;

import com.pricehunter.query.StoreLocationProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface StoreRepository extends JpaRepository<Store, UUID> {

    Optional<Store> findByRetailChainIdAndExternalStoreId(UUID retailChainId, String externalStoreId);

    @Query("""
            select s.id as id,
                   s.externalStoreId as externalStoreId,
                   s.name as name,
                   s.address as address,
                   s.latitude as latitude,
                   s.longitude as longitude
              from Store s
             where s.market.id = :marketId
               and s.active = true
             order by s.id
            """)
    Slice<StoreLocationProjection> findActiveParserLocations(
            @Param("marketId") UUID marketId,
            Pageable pageable);
}
