package com.pricehunter.admin;

import com.pricehunter.parser.ParserSource;
import com.pricehunter.parser.ReviewCandidate;
import com.pricehunter.parser.ReviewCandidateRepository;
import com.pricehunter.parser.ReviewStatus;
import com.pricehunter.parser.ReviewType;
import com.pricehunter.parser.SourceEntityLink;
import com.pricehunter.parser.SourceEntityLinkRepository;
import com.pricehunter.parser.SourceEntityType;
import com.pricehunter.parser.identity.FingerprintService;
import com.pricehunter.shared.ConflictException;
import com.pricehunter.shared.NotFoundException;
import com.pricehunter.shared.SliceResponse;
import com.pricehunter.store.Store;
import com.pricehunter.store.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Открывает очередь ручной проверки и применяет только те решения, для которых уже существует
 * полная бизнес-логика: отклонение и слияние внешнего филиала с известным филиалом Price Hunter.
 */
@Service
@RequiredArgsConstructor
public class AdminReviewService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ReviewCandidateRepository reviewRepository;
    private final StoreRepository storeRepository;
    private final SourceEntityLinkRepository linkRepository;
    private final FingerprintService fingerprintService;

    /** Возвращает очередь выбранного статуса небольшими порциями, начиная с самого старого кандидата. */
    @Transactional(readOnly = true)
    public SliceResponse<ReviewCandidateResponse> findCandidates(ReviewStatus status, int page, int size) {
        return SliceResponse.from(reviewRepository.findByStatusOrderByFirstSeenAtAscIdAsc(status, page(page, size)),
                ReviewCandidateResponse::from);
    }

    /** Возвращает одну карточку кандидата со всеми исходными данными, нужными оператору для решения. */
    @Transactional(readOnly = true)
    public ReviewCandidateResponse findCandidate(UUID candidateId) {
        return ReviewCandidateResponse.from(candidate(candidateId));
    }

    /**
     * Отклоняет ошибочный кандидат либо создаёт реальную связь внешнего филиала с выбранным магазином.
     * APPROVED сознательно не меняет статус: для товара и категории в проекте пока нет логики создания
     * канонической сущности, поэтому такой ответ был бы фиктивным.
     */
    @Transactional
    public ReviewCandidateResponse resolve(UUID candidateId, ResolveReviewCandidateRequest request) {
        ReviewCandidate candidate = candidate(candidateId);
        if (request.decision() == ReviewStatus.PENDING) {
            throw new IllegalArgumentException("Resolution status cannot be PENDING");
        }
        if (request.decision() == ReviewStatus.REJECTED) {
            candidate.resolve(ReviewStatus.REJECTED, null, Instant.now());
            return ReviewCandidateResponse.from(candidate);
        }
        if (request.decision() == ReviewStatus.APPROVED) {
            throw new IllegalArgumentException("APPROVED is unavailable until product and category review actions are implemented");
        }
        if (candidate.getReviewType() != ReviewType.STORE_MATCH) {
            throw new IllegalArgumentException("MERGED is currently supported only for STORE_MATCH candidates");
        }
        if (request.targetEntityId() == null) {
            throw new IllegalArgumentException("targetEntityId is required to merge a store candidate");
        }

        Store store = storeRepository.findById(request.targetEntityId())
                .orElseThrow(() -> new NotFoundException("Store '" + request.targetEntityId() + "' was not found"));
        ParserSource source = candidate.getParserSource();
        if (source.getMarket() == null
                || !store.getRetailChain().getId().equals(source.getMarket().getRetailChain().getId())) {
            throw new IllegalArgumentException("Selected store must belong to the candidate's retail chain");
        }
        if (candidate.getExternalId() == null || candidate.getExternalId().isBlank()) {
            throw new IllegalArgumentException("Store candidate has no external store ID to link");
        }

        linkStore(source, candidate, store);
        candidate.resolve(ReviewStatus.MERGED, store.getId(), Instant.now());
        return ReviewCandidateResponse.from(candidate);
    }

    /** Возвращает кандидата либо единообразную 404-ошибку. */
    private ReviewCandidate candidate(UUID candidateId) {
        return reviewRepository.findById(candidateId)
                .orElseThrow(() -> new NotFoundException("Review candidate '" + candidateId + "' was not found"));
    }

    /** Создаёт устойчивую связь внешнего ID филиала; конфликтующая существующая связь не перезаписывается. */
    private void linkStore(ParserSource source, ReviewCandidate candidate, Store store) {
        linkRepository.findByParserSourceIdAndEntityTypeAndExternalId(
                        source.getId(), SourceEntityType.STORE_LOCATION, candidate.getExternalId())
                .ifPresentOrElse(existing -> {
                    if (!existing.getInternalEntityId().equals(store.getId())) {
                        throw new ConflictException("External store ID is already linked to another store");
                    }
                    existing.observeAgain(candidate.getSourceUrl(), fingerprint(source, candidate, store), Instant.now());
                }, () -> linkRepository.save(new SourceEntityLink(source, SourceEntityType.STORE_LOCATION,
                        candidate.getExternalId(), store.getId(), candidate.getSourceUrl(),
                        fingerprint(source, candidate, store), Instant.now())));
    }

    /** Создаёт воспроизводимый отпечаток ручного сопоставления филиала. */
    private String fingerprint(ParserSource source, ReviewCandidate candidate, Store store) {
        return fingerprintService.sha256(SourceEntityType.STORE_LOCATION + "|" + source.getId()
                + "|" + candidate.getExternalId() + "|" + store.getId());
    }

    /** Ограничивает размер страницы административной очереди безопасным максимумом. */
    private static PageRequest page(int page, int size) {
        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("page must be non-negative and size must be positive");
        }
        return PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
    }
}
