package com.pricehunter.offer;

import com.pricehunter.query.DeliveredOfferProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface ShippingQuoteRepository extends JpaRepository<ShippingQuote, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select quote from ShippingQuote quote
             where quote.offer.id = :offerId
               and quote.destinationCity.id = :destinationCityId
            """)
    Optional<ShippingQuote> findForUpdate(
            @Param("offerId") UUID offerId,
            @Param("destinationCityId") UUID destinationCityId);

    @Query("""
            select offer.id as offerId,
                   chain.name as chainName,
                   sourceCity.name as sourceCityName,
                   coalesce(offer.salePrice, offer.regularPrice) as itemPrice,
                   quote.deliveryPrice as deliveryPrice,
                   (coalesce(offer.salePrice, offer.regularPrice) + coalesce(quote.deliveryPrice, 0)) as totalPrice,
                   quote.minDeliveryDays as minDeliveryDays,
                   quote.maxDeliveryDays as maxDeliveryDays,
                   offer.currency as currency,
                   offer.productUrl as productUrl
              from ShippingQuote quote
              join quote.offer offer
              join offer.market market
              join market.retailChain chain
              join market.city sourceCity
             where offer.productVariant.id = :variantId
               and quote.destinationCity.id = :destinationCityId
               and quote.available = true
               and offer.active = true
               and offer.availabilityStatus in (com.pricehunter.offer.AvailabilityStatus.IN_STOCK,
                                                com.pricehunter.offer.AvailabilityStatus.PREORDER)
               and (offer.regularPrice is not null or offer.salePrice is not null)
             order by (coalesce(offer.salePrice, offer.regularPrice) + coalesce(quote.deliveryPrice, 0)), offer.id
            """)
    Slice<DeliveredOfferProjection> findBestDeliveredOffers(
            @Param("variantId") UUID variantId,
            @Param("destinationCityId") UUID destinationCityId,
            Pageable pageable);
}
