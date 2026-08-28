package com.pricehunter.history;

import com.pricehunter.offer.AvailabilityStatus;
import com.pricehunter.offer.CollectionRun;
import com.pricehunter.offer.Offer;
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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "offer_state_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
/** Интервал времени, в котором цена, наличие и условия предложения оставались неизменными. */
public class OfferStateHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_run_id")
    private CollectionRun sourceRun;

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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "terms_snapshot", nullable = false, columnDefinition = "jsonb")
    private List<Map<String, Object>> termsSnapshot = new ArrayList<>();

    @Column(name = "state_hash", nullable = false, length = 64)
    private String stateHash;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    public OfferStateHistory(Offer offer, CollectionRun sourceRun,
                             List<Map<String, Object>> termsSnapshot, Instant validFrom) {
        this.offer = offer;
        this.sourceRun = sourceRun;
        this.regularPrice = offer.getRegularPrice();
        this.salePrice = offer.getSalePrice();
        this.conditionalPrice = offer.getConditionalPrice();
        this.currency = offer.getCurrency();
        this.availabilityStatus = offer.getAvailabilityStatus();
        this.quantity = offer.getQuantity();
        this.termsSnapshot = termsSnapshot == null ? List.of() : List.copyOf(termsSnapshot);
        this.stateHash = offer.getStateHash();
        this.validFrom = validFrom;
    }

    /** Закрывает текущий интервал перед записью нового состояния. */
    public void closeAt(Instant timestamp) {
        if (!timestamp.isAfter(validFrom)) {
            throw new IllegalArgumentException("History interval must have a positive duration");
        }
        this.validTo = timestamp;
    }
}
