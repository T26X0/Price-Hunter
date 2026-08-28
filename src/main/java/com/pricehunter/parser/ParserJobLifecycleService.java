package com.pricehunter.parser;

import com.pricehunter.offer.CollectionRun;
import com.pricehunter.offer.CollectionRunRepository;
import com.pricehunter.offer.CollectionRunStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
/** Управляет допустимыми переходами задания и синхронизирует их с общим запуском сбора данных. */
public class ParserJobLifecycleService {

    private final ParserSourceRepository sourceRepository;
    private final ParserJobRepository jobRepository;
    private final CollectionRunRepository collectionRunRepository;

    /** Ставит уникальное задание в часовой слот, защищая планировщик от повторной постановки. */
    @Transactional
    public UUID queue(UUID sourceId, ParserJobType jobType, Instant scheduledAt) {
        ParserSource source = sourceRepository.findById(sourceId).orElseThrow();
        Instant slot = scheduledAt.truncatedTo(ChronoUnit.HOURS);
        return jobRepository.save(new ParserJob(source, jobType, slot)).getId();
    }

    /** Запускает задание под блокировкой и создаёт общий {@code CollectionRun} для ценового рынка. */
    @Transactional
    public void start(UUID jobId, Instant startedAt) {
        ParserJob job = jobRepository.findForUpdate(jobId).orElseThrow();
        job.start(startedAt);
        if (job.getMarket() != null) {
            CollectionRun run = collectionRunRepository.save(new CollectionRun(job.getMarket(), startedAt));
            job.attachCollectionRun(run);
        }
    }

    /** Завершает задание, обновляет статистику запуска и дату последнего успешного обхода источника. */
    @Transactional
    public void complete(UUID jobId, ParserJobStatus status, ParserJobOutcome outcome,
                         String errorSummary, Instant completedAt) {
        ParserJob job = jobRepository.findForUpdate(jobId).orElseThrow();
        job.complete(status, outcome, errorSummary, completedAt);
        if (status != ParserJobStatus.FAILED) {
            job.getParserSource().markScanned(job.getJobType(), completedAt);
        }
        if (job.getCollectionRun() != null) {
            job.getCollectionRun().complete(
                    collectionStatus(status),
                    outcome.foundCount(),
                    outcome.changedCount(),
                    outcome.errorCount(),
                    errorSummary,
                    completedAt);
        }
    }

    /** Переводит детальный статус парсера в более общий статус запуска сбора. */
    private static CollectionRunStatus collectionStatus(ParserJobStatus status) {
        return switch (status) {
            case SUCCEEDED, NEEDS_REVIEW -> CollectionRunStatus.SUCCEEDED;
            case PARTIALLY_SUCCEEDED -> CollectionRunStatus.PARTIALLY_SUCCEEDED;
            case FAILED -> CollectionRunStatus.FAILED;
            case QUEUED, RUNNING -> throw new IllegalArgumentException("Parser job is not complete");
        };
    }
}
