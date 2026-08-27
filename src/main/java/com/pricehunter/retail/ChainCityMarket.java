package com.pricehunter.retail;

import com.pricehunter.city.City;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chain_city_markets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChainCityMarket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "retail_chain_id", nullable = false)
    private RetailChain retailChain;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @Enumerated(EnumType.STRING)
    @Column(name = "sales_channel", nullable = false, length = 30)
    private SalesChannel salesChannel;

    @Column(name = "external_market_id", length = 200)
    private String externalMarketId;

    @Column(name = "source_base_url", length = 1000)
    private String sourceBaseUrl;

    @Column(name = "parser_enabled", nullable = false)
    private boolean parserEnabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ChainCityMarket(RetailChain retailChain, City city, SalesChannel salesChannel,
                           String externalMarketId, String sourceBaseUrl, boolean parserEnabled) {
        this.retailChain = retailChain;
        this.city = city;
        this.salesChannel = salesChannel;
        this.externalMarketId = trimToNull(externalMarketId);
        this.sourceBaseUrl = trimToNull(sourceBaseUrl);
        this.parserEnabled = parserEnabled;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
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
