package com.pricehunter.parser;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ParserSchedulePolicyTest {

    private final ParserSchedulePolicy policy = new ParserSchedulePolicy();
    private final Instant now = Instant.parse("2026-08-28T00:00:00Z");

    @Test
    void usesAgreedDailyWeeklyAndBiweeklyIntervals() {
        assertThat(policy.cutoff(ParserJobType.PRICE_REFRESH, now))
                .isEqualTo(Instant.parse("2026-08-27T00:00:00Z"));
        assertThat(policy.cutoff(ParserJobType.PRODUCT_DISCOVERY, now))
                .isEqualTo(Instant.parse("2026-08-21T00:00:00Z"));
        assertThat(policy.cutoff(ParserJobType.STORE_DISCOVERY, now))
                .isEqualTo(Instant.parse("2026-08-14T00:00:00Z"));
    }
}
