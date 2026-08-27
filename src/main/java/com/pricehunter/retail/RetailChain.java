package com.pricehunter.retail;

import com.pricehunter.store.ParserType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "retail_chains")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RetailChain {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(name = "website_url", length = 1000)
    private String websiteUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "parser_type", length = 100)
    private ParserType parserType;

    @Column(name = "parser_enabled", nullable = false)
    private boolean parserEnabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public RetailChain(String name, String code, String websiteUrl, ParserType parserType, boolean parserEnabled) {
        this.name = name.trim();
        this.code = code.trim().toUpperCase(Locale.ROOT);
        this.websiteUrl = trimToNull(websiteUrl);
        this.parserType = parserType;
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
