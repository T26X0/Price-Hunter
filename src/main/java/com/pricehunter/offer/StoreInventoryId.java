package com.pricehunter.offer;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class StoreInventoryId implements Serializable {

    @Column(name = "offer_id")
    private UUID offerId;

    @Column(name = "store_location_id")
    private UUID storeLocationId;
}
