package com.pricehunter.admin;

import com.pricehunter.parser.ReviewCandidate;
import com.pricehunter.parser.ReviewStatus;
import com.pricehunter.parser.ReviewType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Стабильное административное представление элемента очереди ручной проверки. */
public record ReviewCandidateResponse(
        UUID id,
        UUID sourceId,
        UUID jobId,
        ReviewType type,
        ReviewStatus status,
        String candidateName,
        String candidateCategory,
        String sourceUrl,
        String externalId,
        UUID suggestedEntityId,
        BigDecimal confidence,
        String reason,
        Map<String, Object> rawPayload,
        Instant firstSeenAt,
        Instant lastSeenAt,
        Instant resolvedAt
) {
    /** Преобразует загруженного кандидата в DTO, не раскрывая JPA-сущности наружу. */
    static ReviewCandidateResponse from(ReviewCandidate candidate) {
        return new ReviewCandidateResponse(candidate.getId(), candidate.getParserSource().getId(),
                candidate.getParserJob() == null ? null : candidate.getParserJob().getId(),
                candidate.getReviewType(), candidate.getStatus(), candidate.getCandidateName(),
                candidate.getCandidateCategory(), candidate.getSourceUrl(), candidate.getExternalId(),
                candidate.getSuggestedEntityId(), candidate.getConfidence(), candidate.getReason(),
                Map.copyOf(candidate.getRawPayload()), candidate.getFirstSeenAt(), candidate.getLastSeenAt(),
                candidate.getResolvedAt());
    }
}
