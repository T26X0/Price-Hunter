package com.pricehunter.store;

import com.pricehunter.retail.ChainCityMarket;
import com.pricehunter.retail.RetailChain;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "store_locations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "retail_chain_id", nullable = false)
    private RetailChain retailChain;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "market_id", nullable = false)
    private ChainCityMarket market;

    @Column(name = "external_store_id", nullable = false, length = 300)
    private String externalStoreId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "website_url", length = 1000)
    private String websiteUrl;

    @Column(length = 500)
    private String address;

    @Column(precision = 9, scale = 6)
    private java.math.BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private java.math.BigDecimal longitude;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Store(RetailChain retailChain, ChainCityMarket market, String externalStoreId,
                 String name, String address, String websiteUrl) {
        if (!retailChain.equals(market.getRetailChain())) {
            throw new IllegalArgumentException("Store and market must belong to the same retail chain");
        }
        this.retailChain = retailChain;
        this.market = market;
        this.externalStoreId = externalStoreId.trim();
        this.name = name;
        this.address = address;
        this.websiteUrl = websiteUrl;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PrePersist
    void assignCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }
}
