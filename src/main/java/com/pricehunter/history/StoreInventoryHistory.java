package com.pricehunter.history;

import com.pricehunter.offer.AvailabilityStatus;
import com.pricehunter.offer.CollectionRun;
import com.pricehunter.offer.Offer;
import com.pricehunter.store.Store;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "store_inventory_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreInventoryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_location_id", nullable = false)
    private Store storeLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_run_id")
    private CollectionRun sourceRun;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false, length = 30)
    private AvailabilityStatus availabilityStatus;

    private Integer quantity;

    @Column(name = "state_hash", nullable = false, length = 64)
    private String stateHash;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    public StoreInventoryHistory(Offer offer, Store storeLocation, CollectionRun sourceRun,
                                 AvailabilityStatus availabilityStatus, Integer quantity,
                                 String stateHash, Instant validFrom) {
        this.offer = offer;
        this.storeLocation = storeLocation;
        this.sourceRun = sourceRun;
        this.availabilityStatus = availabilityStatus;
        this.quantity = quantity;
        this.stateHash = stateHash;
        this.validFrom = validFrom;
    }

    public void closeAt(Instant timestamp) {
        if (!timestamp.isAfter(validFrom)) {
            throw new IllegalArgumentException("History interval must have a positive duration");
        }
        this.validTo = timestamp;
    }
}
