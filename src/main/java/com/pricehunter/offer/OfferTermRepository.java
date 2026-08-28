package com.pricehunter.offer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Хранилище дополнительных условий предложения и их пакетного сопоставления. */
public interface OfferTermRepository extends JpaRepository<OfferTerm, UUID> {

    @Query("""
            select t from OfferTerm t
             where t.offer.id = :offerId
               and t.active = true
               and (t.validFrom is null or t.validFrom <= :at)
               and (t.validUntil is null or t.validUntil >= :at)
             order by t.termType, t.id
            """)
    List<OfferTerm> findActiveTerms(@Param("offerId") UUID offerId, @Param("at") Instant at);
}
