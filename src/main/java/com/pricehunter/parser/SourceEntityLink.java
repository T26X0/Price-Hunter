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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "source_entity_links")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
/**
 * Устойчивая связь внешнего идентификатора с канонической сущностью Price Hunter.
 * Используется как быстрый первый уровень защиты от дублей при повторных обходах.
 */
public class SourceEntityLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parser_source_id", nullable = false)
    private ParserSource parserSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 30)
    private SourceEntityType entityType;

    @Column(name = "external_id", nullable = false, length = 500)
    private String externalId;

    @Column(name = "internal_entity_id", nullable = false)
    private UUID internalEntityId;

    @Column(name = "source_url", length = 2000)
    private String sourceUrl;

    @Column(nullable = false, length = 64)
    private String fingerprint;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    /** Создаёт первое сопоставление внешнего и внутреннего идентификаторов. */
    public SourceEntityLink(ParserSource parserSource, SourceEntityType entityType, String externalId,
                            UUID internalEntityId, String sourceUrl, String fingerprint, Instant observedAt) {
        this.parserSource = parserSource;
        this.entityType = entityType;
        this.externalId = externalId;
        this.internalEntityId = internalEntityId;
        this.sourceUrl = sourceUrl;
        this.fingerprint = fingerprint;
        this.firstSeenAt = observedAt;
        this.lastSeenAt = observedAt;
    }

    /** Обновляет URL, отпечаток и дату последнего появления уже известной сущности. */
    public void observeAgain(String sourceUrl, String fingerprint, Instant observedAt) {
        this.sourceUrl = sourceUrl;
        this.fingerprint = fingerprint;
        this.lastSeenAt = observedAt;
    }
}
