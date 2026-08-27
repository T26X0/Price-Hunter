package com.pricehunter.offer;

import com.pricehunter.product.ProductVariant;
import com.pricehunter.retail.ChainCityMarket;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "offers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "market_id", nullable = false)
    private ChainCityMarket market;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @Column(name = "external_offer_id", length = 300)
    private String externalOfferId;

    @Column(name = "offer_key", nullable = false, length = 500)
    private String offerKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", nullable = false, length = 30)
    private ConditionType conditionType;

    @Column(name = "condition_grade", length = 50)
    private String conditionGrade;

    @Column(name = "battery_health_percent")
    private Short batteryHealthPercent;

    @Column(length = 500)
    private String completeness;

    @Column(name = "warranty_months")
    private Short warrantyMonths;

    @Column(name = "regular_price", precision = 19, scale = 2)
    private BigDecimal regularPrice;

    @Column(name = "sale_price", precision = 19, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "conditional_price", precision = 19, scale = 2)
    private BigDecimal conditionalPrice;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false, length = 30)
    private AvailabilityStatus availabilityStatus;

    private Integer quantity;

    @Column(name = "product_url", nullable = false, length = 2000)
    private String productUrl;

    @Column(name = "promotion_valid_until")
    private Instant promotionValidUntil;

    @Column(name = "last_checked_at", nullable = false)
    private Instant lastCheckedAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "data_fresh_until")
    private Instant dataFreshUntil;

    @Column(name = "state_hash", nullable = false, length = 64)
    private String stateHash;

    @Column(nullable = false)
    private boolean active = true;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Offer(ChainCityMarket market, ProductVariant productVariant, String externalOfferId,
                 String offerKey, ConditionType conditionType, BigDecimal regularPrice,
                 BigDecimal salePrice, BigDecimal conditionalPrice, String currency,
                 AvailabilityStatus availabilityStatus, Integer quantity, String productUrl,
                 Instant checkedAt, String stateHash) {
        this.market = market;
        this.productVariant = productVariant;
        this.externalOfferId = trimToNull(externalOfferId);
        this.offerKey = offerKey.trim();
        this.conditionType = conditionType;
        this.regularPrice = regularPrice;
        this.salePrice = salePrice;
        this.conditionalPrice = conditionalPrice;
        this.currency = currency.trim().toUpperCase();
        this.availabilityStatus = availabilityStatus;
        this.quantity = quantity;
        this.productUrl = productUrl;
        this.lastCheckedAt = checkedAt;
        this.lastSeenAt = availabilityStatus == AvailabilityStatus.OUT_OF_STOCK ? null : checkedAt;
        this.stateHash = stateHash;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void refresh(BigDecimal regularPrice, BigDecimal salePrice, BigDecimal conditionalPrice,
                        AvailabilityStatus availabilityStatus, Integer quantity, String productUrl,
                        Instant checkedAt, Instant freshUntil, String stateHash) {
        this.regularPrice = regularPrice;
        this.salePrice = salePrice;
        this.conditionalPrice = conditionalPrice;
        this.availabilityStatus = availabilityStatus;
        this.quantity = quantity;
        this.productUrl = productUrl;
        this.lastCheckedAt = checkedAt;
        if (availabilityStatus != AvailabilityStatus.OUT_OF_STOCK) {
            this.lastSeenAt = checkedAt;
        }
        this.dataFreshUntil = freshUntil;
        this.stateHash = stateHash;
        this.active = true;
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

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
