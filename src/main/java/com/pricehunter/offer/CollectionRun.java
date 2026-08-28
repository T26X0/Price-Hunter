package com.pricehunter.offer;

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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "collection_runs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
/** Один общий проход сбора цен и остатков по городскому рынку сети. */
public class CollectionRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "market_id", nullable = false)
    private ChainCityMarket market;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CollectionRunStatus status;

    @Column(name = "found_count", nullable = false)
    private int foundCount;

    @Column(name = "changed_count", nullable = false)
    private int changedCount;

    @Column(name = "error_count", nullable = false)
    private int errorCount;

    @Column(name = "error_summary", length = 2000)
    private String errorSummary;

    /** Создаёт выполняющийся запуск для рынка. */
    public CollectionRun(ChainCityMarket market, Instant startedAt) {
        this.market = market;
        this.startedAt = startedAt;
        this.status = CollectionRunStatus.RUNNING;
    }

    /** Завершает запуск и сохраняет агрегированные счётчики. */
    public void complete(CollectionRunStatus status, int foundCount, int changedCount,
                         int errorCount, String errorSummary, Instant completedAt) {
        if (status == CollectionRunStatus.RUNNING) {
            throw new IllegalArgumentException("A completed run cannot remain RUNNING");
        }
        this.status = status;
        this.foundCount = foundCount;
        this.changedCount = changedCount;
        this.errorCount = errorCount;
        this.errorSummary = errorSummary;
        this.completedAt = completedAt;
    }
}
