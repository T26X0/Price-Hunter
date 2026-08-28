package com.pricehunter.offer;

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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "offer_terms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
/** Дополнительное условие предложения: подарок, рассрочка, кредит или скидка. */
public class OfferTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;

    @Enumerated(EnumType.STRING)
    @Column(name = "term_type", nullable = false, length = 30)
    private OfferTermType termType;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "monetary_value", precision = 19, scale = 2)
    private BigDecimal monetaryValue;

    @Column(name = "annual_rate", precision = 8, scale = 4)
    private BigDecimal annualRate;

    @Column(name = "term_months")
    private Short termMonths;

    @Column(name = "monthly_payment", precision = 19, scale = 2)
    private BigDecimal monthlyPayment;

    @Column(name = "initial_payment", precision = 19, scale = 2)
    private BigDecimal initialPayment;

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_until")
    private Instant validUntil;

    @Column(name = "terms_url", length = 2000)
    private String termsUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Создаёт активное коммерческое условие. */
    public OfferTerm(Offer offer, OfferTermType termType, String title, String description,
                     BigDecimal monetaryValue, BigDecimal annualRate, Short termMonths,
                     BigDecimal monthlyPayment, BigDecimal initialPayment, Instant validFrom,
                     Instant validUntil, String termsUrl, Map<String, Object> metadata) {
        this.offer = offer;
        this.termType = termType;
        this.title = title.trim();
        this.description = trimToNull(description);
        this.monetaryValue = monetaryValue;
        this.annualRate = annualRate;
        this.termMonths = termMonths;
        this.monthlyPayment = monthlyPayment;
        this.initialPayment = initialPayment;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.termsUrl = trimToNull(termsUrl);
        this.metadata = metadata == null ? new HashMap<>() : new HashMap<>(metadata);
    }

    /** Помечает исчезнувшее условие неактивным без удаления истории. */
    public void deactivate() {
        this.active = false;
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
