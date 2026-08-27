package com.pricehunter.history;

import com.pricehunter.query.PriceHistoryPointProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OfferStateHistoryRepository extends JpaRepository<OfferStateHistory, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from OfferStateHistory h where h.offer.id = :offerId and h.validTo is null")
    Optional<OfferStateHistory> findOpenStateForUpdate(@Param("offerId") UUID offerId);

    @Query("""
            select h.id as id,
                   h.regularPrice as regularPrice,
                   h.salePrice as salePrice,
                   h.conditionalPrice as conditionalPrice,
                   h.currency as currency,
                   h.availabilityStatus as availabilityStatus,
                   h.quantity as quantity,
                   h.validFrom as validFrom,
                   h.validTo as validTo
              from OfferStateHistory h
             where h.offer.id = :offerId
               and h.validFrom < :to
               and (h.validTo is null or h.validTo > :from)
             order by h.validFrom
            """)
    List<PriceHistoryPointProjection> findChartRange(
            @Param("offerId") UUID offerId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from OfferStateHistory h where h.validTo is not null and h.validTo < :cutoff")
    int deleteClosedHistoryBefore(@Param("cutoff") Instant cutoff);
}
