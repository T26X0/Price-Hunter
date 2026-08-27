package com.pricehunter.offer;

import com.pricehunter.retail.ChainCityMarket;
import com.pricehunter.store.Store;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "store_inventories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreInventory {

    @EmbeddedId
    private StoreInventoryId id;

    @MapsId("offerId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;

    @MapsId("storeLocationId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_location_id", nullable = false)
    private Store storeLocation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "market_id", nullable = false)
    private ChainCityMarket market;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false, length = 30)
    private AvailabilityStatus availabilityStatus;

    private Integer quantity;

    @Column(name = "last_checked_at", nullable = false)
    private Instant lastCheckedAt;

    @Column(name = "state_hash", nullable = false, length = 64)
    private String stateHash;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public StoreInventory(Offer offer, Store storeLocation, AvailabilityStatus availabilityStatus,
                          Integer quantity, Instant checkedAt, String stateHash) {
        if (!offer.getMarket().equals(storeLocation.getMarket())) {
            throw new IllegalArgumentException("Offer and store location must belong to the same market");
        }
        this.offer = offer;
        this.storeLocation = storeLocation;
        this.id = new StoreInventoryId(offer.getId(), storeLocation.getId());
        this.market = offer.getMarket();
        this.availabilityStatus = availabilityStatus;
        this.quantity = quantity;
        this.lastCheckedAt = checkedAt;
        this.stateHash = stateHash;
        this.updatedAt = Instant.now();
    }

    public void refresh(AvailabilityStatus status, Integer quantity, Instant checkedAt, String stateHash) {
        this.availabilityStatus = status;
        this.quantity = quantity;
        this.lastCheckedAt = checkedAt;
        this.stateHash = stateHash;
    }

    @PreUpdate
    void assignUpdatedAt() {
        updatedAt = Instant.now();
    }
}
