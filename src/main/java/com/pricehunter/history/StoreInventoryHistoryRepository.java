package com.pricehunter.history;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Запросы интервальной истории наличия товара в филиалах. */
public interface StoreInventoryHistoryRepository extends JpaRepository<StoreInventoryHistory, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select h from StoreInventoryHistory h
             where h.offer.id = :offerId
               and h.storeLocation.id = :storeLocationId
               and h.validTo is null
            """)
    Optional<StoreInventoryHistory> findOpenStateForUpdate(
            @Param("offerId") UUID offerId,
            @Param("storeLocationId") UUID storeLocationId);

    @Query("""
            select h from StoreInventoryHistory h
             where h.offer.id = :offerId
               and h.storeLocation.id = :storeLocationId
               and h.validFrom < :to
               and (h.validTo is null or h.validTo > :from)
             order by h.validFrom
            """)
    List<StoreInventoryHistory> findChartRange(
            @Param("offerId") UUID offerId,
            @Param("storeLocationId") UUID storeLocationId,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
