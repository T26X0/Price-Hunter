package com.pricehunter.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "attribute_definitions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttributeDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 20)
    private AttributeDataType dataType;

    @Column(length = 30)
    private String unit;

    @Column(nullable = false)
    private boolean filterable;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AttributeDefinition(String code, String name, AttributeDataType dataType,
                               String unit, boolean filterable) {
        this.code = code.trim().toLowerCase(Locale.ROOT);
        this.name = name.trim();
        this.dataType = dataType;
        this.unit = unit == null || unit.isBlank() ? null : unit.trim();
        this.filterable = filterable;
        this.createdAt = Instant.now();
    }

    @PrePersist
    void assignCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
