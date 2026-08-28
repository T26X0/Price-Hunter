package com.pricehunter.city;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "cities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
/** Канонический город, общий для сетей, филиалов, рынков и расчётов доставки. */
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 200)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 200)
    private String normalizedName;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(length = 100)
    private String timezone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Создаёт город с российскими значениями страны и часового пояса по умолчанию. */
    public City(String name) {
        this(name, "RU", null);
    }

    /** Создаёт город с явной страной и часовым поясом. */
    public City(String name, String countryCode, String timezone) {
        this.name = name.trim();
        this.normalizedName = normalize(name);
        this.countryCode = countryCode.trim().toUpperCase(Locale.ROOT);
        this.timezone = timezone;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Заполняет даты создания и изменения перед первой записью. */
    @PrePersist
    void assignCreatedAt() {
        normalizedName = normalize(name);
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    /** Обновляет техническую дату изменения. */
    @PreUpdate
    void assignUpdatedAt() {
        normalizedName = normalize(name);
        updatedAt = Instant.now();
    }

    /** Нормализует название для регистронезависимого поиска дублей. */
    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
