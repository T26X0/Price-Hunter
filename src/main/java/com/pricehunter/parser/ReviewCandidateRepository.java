package com.pricehunter.parser;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Хранилище очереди ручной проверки с поиском повторных кандидатов по отпечатку. */
public interface ReviewCandidateRepository extends JpaRepository<ReviewCandidate, UUID> {

    /** Ищет ранее созданную карточку той же неоднозначности у того же источника. */
    Optional<ReviewCandidate> findByParserSourceIdAndReviewTypeAndFingerprint(
            UUID parserSourceId, ReviewType reviewType, String fingerprint);

    Slice<ReviewCandidate> findByStatusOrderByFirstSeenAtAscIdAsc(
            ReviewStatus status, Pageable pageable);
}
    /** Постранично выдаёт кандидатов выбранного статуса, начиная с самых старых. */
