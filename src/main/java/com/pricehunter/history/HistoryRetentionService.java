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
public class HistoryRetentionService {

    private final HistoryRetentionRepository retentionRepository;

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

    public Instant sixMonthCutoff(Clock clock) {
        return clock.instant()
                .atZone(ZoneOffset.UTC)
                .minusMonths(6)
                .withDayOfMonth(1)
                .truncatedTo(ChronoUnit.DAYS)
                .toInstant();
    }

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
