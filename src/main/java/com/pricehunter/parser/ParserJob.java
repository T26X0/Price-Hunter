package com.pricehunter.parser;

import com.pricehunter.offer.CollectionRun;
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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "parser_jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
/**
 * Один плановый или ручной запуск парсера.
 * Сущность хранит жизненный цикл, счётчики результата, ошибку и связанную срезку сбора цен.
 */
public class ParserJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parser_source_id", nullable = false)
    private ParserSource parserSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "market_id")
    private ChainCityMarket market;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_run_id")
    private CollectionRun collectionRun;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 30)
    private ParserJobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ParserJobStatus status;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "attempt_count", nullable = false)
    private short attemptCount;

    @Column(name = "max_attempts", nullable = false)
    private short maxAttempts = 3;

    @Column(name = "found_count", nullable = false)
    private int foundCount;

    @Column(name = "created_count", nullable = false)
    private int createdCount;

    @Column(name = "changed_count", nullable = false)
    private int changedCount;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Column(name = "error_count", nullable = false)
    private int errorCount;

    @Column(name = "error_summary", length = 2000)
    private String errorSummary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Создаёт задание в состоянии очереди для конкретного источника и часового слота. */
    public ParserJob(ParserSource parserSource, ParserJobType jobType, Instant scheduledAt) {
        this.parserSource = parserSource;
        this.market = parserSource.getMarket();
        this.jobType = jobType;
        this.scheduledAt = scheduledAt;
        this.status = ParserJobStatus.QUEUED;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Переводит задание из очереди в работу и увеличивает номер попытки. */
    public void start(Instant startedAt) {
        if (status != ParserJobStatus.QUEUED) {
            throw new IllegalStateException("Only a queued parser job can start");
        }
        this.status = ParserJobStatus.RUNNING;
        this.startedAt = startedAt;
        this.attemptCount++;
    }

    /** Связывает задание с общим запуском сбора данных по рынку. */
    public void attachCollectionRun(CollectionRun collectionRun) {
        this.collectionRun = collectionRun;
    }

    /** Завершает задание терминальным статусом и фиксирует итоговые счётчики. */
    public void complete(ParserJobStatus status, ParserJobOutcome outcome,
                         String errorSummary, Instant completedAt) {
        if (status == ParserJobStatus.QUEUED || status == ParserJobStatus.RUNNING) {
            throw new IllegalArgumentException("Completed parser job requires a terminal status");
        }
        this.status = status;
        this.foundCount = outcome.foundCount();
        this.createdCount = outcome.createdCount();
        this.changedCount = outcome.changedCount();
        this.reviewCount = outcome.reviewCount();
        this.errorCount = outcome.errorCount();
        this.errorSummary = errorSummary;
        this.completedAt = completedAt;
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

    /** Обновляет дату изменения перед сохранением нового состояния задания. */
    @PreUpdate
    void assignUpdatedAt() {
        updatedAt = Instant.now();
    }
}
