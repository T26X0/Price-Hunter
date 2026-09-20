package com.pricehunter.admin;

import com.pricehunter.parser.ParserJob;
import com.pricehunter.parser.ParserJobStatus;
import com.pricehunter.parser.ParserJobType;

import java.time.Instant;
import java.util.UUID;

/** Публичный итог ручного или планового задания парсера. */
public record ParserJobResponse(
        UUID id,
        UUID sourceId,
        UUID marketId,
        ParserJobType type,
        ParserJobStatus status,
        int found,
        int created,
        int changed,
        int review,
        int errors,
        String errorSummary,
        Instant scheduledAt,
        Instant startedAt,
        Instant completedAt
) {
    /** Преобразует сущность задания в стабильный административный DTO. */
    static ParserJobResponse from(ParserJob job) {
        return new ParserJobResponse(job.getId(), job.getParserSource().getId(),
                job.getMarket() == null ? null : job.getMarket().getId(), job.getJobType(), job.getStatus(),
                job.getFoundCount(), job.getCreatedCount(), job.getChangedCount(), job.getReviewCount(),
                job.getErrorCount(), job.getErrorSummary(), job.getScheduledAt(), job.getStartedAt(), job.getCompletedAt());
    }
}
