package com.pricehunter.product;

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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_variants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
/** Конкретная фильтруемая конфигурация модели, например iPhone 16 Pro 256 GB Black. */
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_model_id", nullable = false)
    private Product productModel;

    @Column(name = "canonical_key", nullable = false, length = 500)
    private String canonicalKey;

    @Column(name = "manufacturer_sku", length = 160)
    private String manufacturerSku;

    @Column(name = "display_name", nullable = false, length = 300)
    private String displayName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Создаёт конфигурацию с каноническим ключом и необязательным артикулом производителя. */
    public ProductVariant(Product productModel, String canonicalKey, String manufacturerSku, String displayName) {
        this.productModel = productModel;
        this.canonicalKey = canonicalKey.trim();
        this.manufacturerSku = trimToNull(manufacturerSku);
        this.displayName = displayName.trim();
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Заполняет даты создания и изменения перед первой записью. */
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

    /** Обновляет техническую дату изменения. */
    @PreUpdate
    void assignUpdatedAt() {
        updatedAt = Instant.now();
    }

    /** Преобразует пустую необязательную строку в {@code null}. */
    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
