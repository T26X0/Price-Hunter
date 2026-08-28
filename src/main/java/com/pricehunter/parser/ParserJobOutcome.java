package com.pricehunter.parser;

/** Итоговые счётчики одного задания, которые сохраняются и в задании, и в общем запуске сбора. */
public record ParserJobOutcome(
        int foundCount,
        int createdCount,
        int changedCount,
        int reviewCount,
        int errorCount
) {
    /** Не допускает отрицательные значения статистики. */
    public ParserJobOutcome {
        if (foundCount < 0 || createdCount < 0 || changedCount < 0 || reviewCount < 0 || errorCount < 0) {
            throw new IllegalArgumentException("Parser job counters cannot be negative");
        }
    }

    /** @return нулевой результат для запуска, завершившегося до чтения позиций */
    public static ParserJobOutcome empty() {
        return new ParserJobOutcome(0, 0, 0, 0, 0);
    }
}
