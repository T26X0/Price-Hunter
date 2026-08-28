package com.pricehunter.parser;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "parser_sources")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
/**
 * Настраиваемый внешний источник парсинга, привязанный к рынку сети в городе.
 * Хранит выбор коннектора, URL, параметры и даты последних успешных запусков.
 */
public class ParserSource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "market_id")
    private ChainCityMarket market;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private ParserSourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "connector_mode", nullable = false, length = 20)
    private ConnectorMode connectorMode;

    @Column(name = "connector_key", nullable = false, length = 100)
    private String connectorKey;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "base_url", nullable = false, length = 1000)
    private String baseUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> configuration = new HashMap<>();

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "last_price_scan_at")
    private Instant lastPriceScanAt;

    @Column(name = "last_product_scan_at")
    private Instant lastProductScanAt;

    @Column(name = "last_store_scan_at")
    private Instant lastStoreScanAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Создаёт конфигурацию источника и нормализует ключ коннектора. */
    public ParserSource(ChainCityMarket market, ParserSourceType sourceType, ConnectorMode connectorMode,
                        String connectorKey, String name, String baseUrl,
                        Map<String, Object> configuration) {
        this.market = market;
        this.sourceType = sourceType;
        this.connectorMode = connectorMode;
        this.connectorKey = connectorKey.trim().toLowerCase(Locale.ROOT);
        this.name = name.trim();
        this.baseUrl = baseUrl.trim();
        this.configuration = configuration == null ? new HashMap<>() : new HashMap<>(configuration);
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Запоминает успешный проход указанного типа для расчёта следующего запуска.
     * Поиск товаров одновременно считается обновлением их цен.
     */
    public void markScanned(ParserJobType jobType, Instant scannedAt) {
        switch (jobType) {
            case PRICE_REFRESH -> lastPriceScanAt = scannedAt;
            case PRODUCT_DISCOVERY -> {
                lastProductScanAt = scannedAt;
                lastPriceScanAt = scannedAt;
            }
            case STORE_DISCOVERY -> lastStoreScanAt = scannedAt;
        }
    }

    /** Заполняет даты создания и изменения перед первой записью сущности. */
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

    /** Обновляет техническую дату изменения перед SQL UPDATE. */
    @PreUpdate
    void assignUpdatedAt() {
        updatedAt = Instant.now();
    }
}
