package com.pricehunter.offer;

import com.pricehunter.query.InventoryProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/** Запросы текущих остатков филиалов и блокировки строк при импорте. */
public interface StoreInventoryRepository extends JpaRepository<StoreInventory, StoreInventoryId> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select inventory from StoreInventory inventory
             where inventory.offer.id = :offerId
               and inventory.storeLocation.id = :storeLocationId
            """)
    Optional<StoreInventory> findForUpdate(
            @Param("offerId") UUID offerId,
            @Param("storeLocationId") UUID storeLocationId);

    @Query("""
            select store.id as storeLocationId,
                   store.name as storeName,
                   store.address as address,
                   inventory.availabilityStatus as availabilityStatus,
                   inventory.quantity as quantity,
                   inventory.lastCheckedAt as lastCheckedAt
              from StoreInventory inventory
              join inventory.storeLocation store
             where inventory.offer.id = :offerId
             order by case inventory.availabilityStatus
                          when com.pricehunter.offer.AvailabilityStatus.IN_STOCK then 0
                          when com.pricehunter.offer.AvailabilityStatus.PREORDER then 1
                          when com.pricehunter.offer.AvailabilityStatus.UNKNOWN then 2
                          else 3
                      end,
                      store.name,
                      store.id
            """)
    Slice<InventoryProjection> findOfferInventory(
            @Param("offerId") UUID offerId,
            Pageable pageable);
}
