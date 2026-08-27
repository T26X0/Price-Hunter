package com.pricehunter.offer;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OfferStateHasherTest {

    private final OfferStateHasher hasher = new OfferStateHasher();

    @Test
    void ignoresMapAndTermOrdering() {
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("title", "Case");
        first.put("type", "GIFT");
        Map<String, Object> reordered = new LinkedHashMap<>();
        reordered.put("type", "GIFT");
        reordered.put("title", "Case");

        OfferSnapshot left = snapshot(new BigDecimal("115000.00"), List.of(first, Map.of("type", "CREDIT")));
        OfferSnapshot right = snapshot(new BigDecimal("115000"), List.of(Map.of("type", "CREDIT"), reordered));

        assertThat(hasher.hash(left)).isEqualTo(hasher.hash(right));
    }

    @Test
    void changesWhenPriceChanges() {
        assertThat(hasher.hash(snapshot(new BigDecimal("115000"), List.of())))
                .isNotEqualTo(hasher.hash(snapshot(new BigDecimal("119000"), List.of())));
    }

    private static OfferSnapshot snapshot(BigDecimal price, List<Map<String, Object>> terms) {
        return new OfferSnapshot(
                UUID.randomUUID(), UUID.randomUUID(), "external-1", "variant:new",
                ConditionType.NEW, price, null, null, "RUB",
                AvailabilityStatus.IN_STOCK, 5, "https://example.test/product",
                Instant.parse("2026-08-27T00:00:00Z"), null, terms);
    }
}
