package com.pricehunter.history;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
/** Агрегирует детальную историю старше шести месяцев и безопасно удаляет исходные интервалы. */
public class HistoryRetentionService {

    private final HistoryRetentionRepository retentionRepository;

    /** Строит месячные агрегаты, разрезает пограничные интервалы и очищает старые детали. */
    @Transactional
    public RetentionResult aggregateAndPrune(Instant cutoff) {
        int offerMonths = retentionRepository.aggregateOfferMonths(cutoff);
        int inventoryMonths = retentionRepository.aggregateInventoryMonths(cutoff);
        int offerAnchors = retentionRepository.splitOfferIntervalsAt(cutoff);
        int inventoryAnchors = retentionRepository.splitInventoryIntervalsAt(cutoff);
        int offerRows = retentionRepository.deleteClosedOfferHistory(cutoff);
        int inventoryRows = retentionRepository.deleteClosedInventoryHistory(cutoff);
        return new RetentionResult(offerMonths, inventoryMonths, offerAnchors, inventoryAnchors,
                offerRows, inventoryRows);
    }

    /** Вычисляет календарную границу хранения относительно переданных часов. */
    public Instant sixMonthCutoff(Clock clock) {
        return clock.instant()
                .atZone(ZoneOffset.UTC)
                .minusMonths(6)
                .withDayOfMonth(1)
                .truncatedTo(ChronoUnit.DAYS)
                .toInstant();
    }

    /** Итоговые количества агрегированных, разделённых и удалённых строк. */
    public record RetentionResult(
            int aggregatedOfferMonths,
            int aggregatedInventoryMonths,
            int anchoredOfferIntervals,
            int anchoredInventoryIntervals,
            int deletedOfferHistoryRows,
            int deletedInventoryHistoryRows
    ) {
    }
}
