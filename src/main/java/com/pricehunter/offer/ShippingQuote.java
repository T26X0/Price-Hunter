package com.pricehunter.offer;

import com.pricehunter.city.City;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shipping_quotes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShippingQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_city_id", nullable = false)
    private City destinationCity;

    @Column(nullable = false)
    private boolean available;

    @Column(name = "delivery_price", precision = 19, scale = 2)
    private BigDecimal deliveryPrice;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "min_delivery_days")
    private Short minDeliveryDays;

    @Column(name = "max_delivery_days")
    private Short maxDeliveryDays;

    @Column(name = "last_checked_at", nullable = false)
    private Instant lastCheckedAt;

    @Column(name = "state_hash", nullable = false, length = 64)
    private String stateHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ShippingQuote(Offer offer, City destinationCity, boolean available,
                         BigDecimal deliveryPrice, String currency, Short minDeliveryDays,
                         Short maxDeliveryDays, Instant checkedAt, String stateHash) {
        this.offer = offer;
        this.destinationCity = destinationCity;
        refresh(available, deliveryPrice, currency, minDeliveryDays, maxDeliveryDays, checkedAt, stateHash);
    }

    public void refresh(boolean available, BigDecimal deliveryPrice, String currency,
                        Short minDeliveryDays, Short maxDeliveryDays, Instant checkedAt,
                        String stateHash) {
        this.available = available;
        this.deliveryPrice = deliveryPrice;
        this.currency = currency.trim().toUpperCase();
        this.minDeliveryDays = minDeliveryDays;
        this.maxDeliveryDays = maxDeliveryDays;
        this.lastCheckedAt = checkedAt;
        this.stateHash = stateHash;
    }

    @PrePersist
    void assignTimestamps() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void assignUpdatedAt() {
        updatedAt = Instant.now();
    }
}
