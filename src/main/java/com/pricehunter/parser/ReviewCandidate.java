package com.pricehunter.parser;

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
@Table(name = "review_candidates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
/**
 * Неоднозначный магазин, товар или категория, которые нельзя безопасно объединить автоматически.
 * Повторные наблюдения обновляют одну карточку по отпечатку, а не создают дубли.
 */
public class ReviewCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parser_source_id", nullable = false)
    private ParserSource parserSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parser_job_id")
    private ParserJob parserJob;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_type", nullable = false, length = 40)
    private ReviewType reviewType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewStatus status = ReviewStatus.PENDING;

    @Column(nullable = false, length = 64)
    private String fingerprint;

    @Column(name = "candidate_name", nullable = false, length = 500)
    private String candidateName;

    @Column(name = "candidate_category", length = 150)
    private String candidateCategory;

    @Column(name = "source_url", length = 2000)
    private String sourceUrl;

    @Column(name = "external_id", length = 500)
    private String externalId;

    @Column(name = "suggested_entity_id")
    private UUID suggestedEntityId;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(nullable = false, length = 1000)
    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> rawPayload = new HashMap<>();

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Создаёт новую ожидающую проверки карточку с исходными данными источника. */
    public ReviewCandidate(ParserSource parserSource, ParserJob parserJob, ReviewType reviewType,
                           String fingerprint, String candidateName, String candidateCategory,
                           String sourceUrl, String externalId, UUID suggestedEntityId,
                           BigDecimal confidence, String reason, Map<String, Object> rawPayload,
                           Instant observedAt) {
        this.parserSource = parserSource;
        this.parserJob = parserJob;
        this.reviewType = reviewType;
        this.fingerprint = fingerprint;
        this.candidateName = candidateName;
        this.candidateCategory = candidateCategory;
        this.sourceUrl = sourceUrl;
        this.externalId = externalId;
        this.suggestedEntityId = suggestedEntityId;
        this.confidence = confidence;
        this.reason = reason;
        this.rawPayload = rawPayload == null ? new HashMap<>() : new HashMap<>(rawPayload);
        this.firstSeenAt = observedAt;
        this.lastSeenAt = observedAt;
    }

    /** Обновляет существующего кандидата свежими данными и повторно открывает закрытую проверку. */
    public void observeAgain(ParserJob parserJob, String sourceUrl, Map<String, Object> rawPayload,
                             String reason, Instant observedAt) {
        this.parserJob = parserJob;
        this.sourceUrl = sourceUrl;
        this.rawPayload = rawPayload == null ? new HashMap<>() : new HashMap<>(rawPayload);
        this.reason = reason;
        this.lastSeenAt = observedAt;
        if (status != ReviewStatus.PENDING) {
            this.status = ReviewStatus.PENDING;
            this.resolvedAt = null;
        }
    }

    /** Фиксирует решение оператора и при необходимости идентификатор выбранной сущности. */
    public void resolve(ReviewStatus status, UUID resolvedEntityId, Instant resolvedAt) {
        if (status == ReviewStatus.PENDING) {
            throw new IllegalArgumentException("Resolution status cannot be PENDING");
        }
        this.status = status;
        this.suggestedEntityId = resolvedEntityId;
        this.resolvedAt = resolvedAt;
    }

    /** Заполняет технические даты перед первой записью. */
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

    /** Обновляет дату изменения при повторном наблюдении или решении оператора. */
    @PreUpdate
    void assignUpdatedAt() {
        updatedAt = Instant.now();
    }
}
