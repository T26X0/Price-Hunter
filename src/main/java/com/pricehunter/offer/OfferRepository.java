package com.pricehunter.offer;

import com.pricehunter.query.OfferCardProjection;
import com.pricehunter.query.OfferIdentityProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Оптимизированные запросы текущих предложений, блокировок импорта и карточек сравнения. */
public interface OfferRepository extends JpaRepository<Offer, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select o from Offer o
             where o.market.id = :marketId
               and ((:storeLocationId is null and o.storeLocation is null)
                    or o.storeLocation.id = :storeLocationId)
               and o.externalOfferId = :externalOfferId
            """)
    Optional<Offer> findForUpdateByExternalId(
            @Param("marketId") UUID marketId,
            @Param("storeLocationId") UUID storeLocationId,
            @Param("externalOfferId") String externalOfferId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select o from Offer o
             where o.market.id = :marketId
               and ((:storeLocationId is null and o.storeLocation is null)
                    or o.storeLocation.id = :storeLocationId)
               and o.offerKey = :offerKey
            """)
    Optional<Offer> findForUpdateByOfferKey(
            @Param("marketId") UUID marketId,
            @Param("storeLocationId") UUID storeLocationId,
            @Param("offerKey") String offerKey);

    @Query("""
            select o.id as id,
                   o.externalOfferId as externalOfferId,
                   o.offerKey as offerKey,
                   o.stateHash as stateHash,
                   o.lastCheckedAt as lastCheckedAt
              from Offer o
             where o.market.id = :marketId
               and ((:storeLocationId is null and o.storeLocation is null)
                    or o.storeLocation.id = :storeLocationId)
               and o.externalOfferId in :externalOfferIds
            """)
    List<OfferIdentityProjection> findExistingIdentities(
            @Param("marketId") UUID marketId,
            @Param("storeLocationId") UUID storeLocationId,
            @Param("externalOfferIds") Collection<String> externalOfferIds);

    @Query("""
            select o.id as offerId,
                   market.id as marketId,
                   chain.id as chainId,
                   chain.name as chainName,
                   city.id as cityId,
                   city.name as cityName,
                   store.id as storeLocationId,
                   store.name as storeLocationName,
                   store.address as storeLocationAddress,
                   product.id as productModelId,
                   product.name as productName,
                   variant.id as variantId,
                   variant.displayName as variantName,
                   o.conditionType as conditionType,
                   o.regularPrice as regularPrice,
                   o.salePrice as salePrice,
                   o.conditionalPrice as conditionalPrice,
                   o.currency as currency,
                   o.availabilityStatus as availabilityStatus,
                   o.quantity as quantity,
                   o.productUrl as productUrl,
                   o.lastCheckedAt as lastCheckedAt
              from Offer o
              join o.market market
              join market.retailChain chain
              join market.city city
              left join o.storeLocation store
              join o.productVariant variant
              join variant.productModel product
             where o.id = :offerId
               and o.active = true
            """)
    Optional<OfferCardProjection> findCard(@Param("offerId") UUID offerId);

    @Query("""
            select o.id as offerId,
                   market.id as marketId,
                   chain.id as chainId,
                   chain.name as chainName,
                   city.id as cityId,
                   city.name as cityName,
                   store.id as storeLocationId,
                   store.name as storeLocationName,
                   store.address as storeLocationAddress,
                   product.id as productModelId,
                   product.name as productName,
                   variant.id as variantId,
                   variant.displayName as variantName,
                   o.conditionType as conditionType,
                   o.regularPrice as regularPrice,
                   o.salePrice as salePrice,
                   o.conditionalPrice as conditionalPrice,
                   o.currency as currency,
                   o.availabilityStatus as availabilityStatus,
                   o.quantity as quantity,
                   o.productUrl as productUrl,
                   o.lastCheckedAt as lastCheckedAt
              from Offer o
              join o.market market
              join market.retailChain chain
              join market.city city
              left join o.storeLocation store
              join o.productVariant variant
              join variant.productModel product
             where variant.id = :variantId
               and market.city.id = :cityId
               and o.active = true
               and o.availabilityStatus in (com.pricehunter.offer.AvailabilityStatus.IN_STOCK,
                                            com.pricehunter.offer.AvailabilityStatus.PREORDER)
             order by coalesce(o.salePrice, o.regularPrice) asc, store.id, o.id
            """)
    Slice<OfferCardProjection> findBestLocalOffers(
            @Param("variantId") UUID variantId,
            @Param("cityId") UUID cityId,
            Pageable pageable);

    @Query("""
            select o.id as id,
                   o.externalOfferId as externalOfferId,
                   o.offerKey as offerKey,
                   o.stateHash as stateHash,
                   o.lastCheckedAt as lastCheckedAt
              from Offer o
             where o.market.id = :marketId
               and o.active = true
               and (o.dataFreshUntil is null or o.dataFreshUntil < :now)
             order by o.lastCheckedAt, o.id
            """)
    Slice<OfferIdentityProjection> findStaleOffers(
            @Param("marketId") UUID marketId,
            @Param("now") Instant now,
            Pageable pageable);
}
