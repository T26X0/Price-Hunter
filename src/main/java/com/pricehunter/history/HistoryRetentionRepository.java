package com.pricehunter.history;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;

@Repository
@RequiredArgsConstructor
/** Низкоуровневые SQL-операции агрегации и очистки больших объёмов истории. */
public class HistoryRetentionRepository {

    private final JdbcTemplate jdbcTemplate;

    /** Строит или обновляет месячную статистику цен до границы хранения. */
    public int aggregateOfferMonths(Instant cutoff) {
        return jdbcTemplate.update("""
                INSERT INTO offer_monthly_stats (
                    offer_id, month, currency, average_effective_price,
                    minimum_effective_price, maximum_effective_price,
                    first_effective_price, last_effective_price,
                    in_stock_days, out_of_stock_days, unknown_days, aggregated_at
                )
                WITH segments AS (
                    SELECT h.offer_id,
                           (month_start AT TIME ZONE 'UTC')::date AS month,
                           h.currency,
                           COALESCE(h.sale_price, h.regular_price) AS effective_price,
                           h.availability_status,
                           GREATEST(h.valid_from, month_start) AS segment_start,
                           LEAST(COALESCE(h.valid_to, ?::timestamptz), month_start + INTERVAL '1 month') AS segment_end
                    FROM offer_state_history h
                    CROSS JOIN LATERAL GENERATE_SERIES(
                        DATE_TRUNC('month', h.valid_from AT TIME ZONE 'UTC') AT TIME ZONE 'UTC',
                        DATE_TRUNC('month', (LEAST(COALESCE(h.valid_to, ?::timestamptz), ?::timestamptz)
                            - INTERVAL '1 microsecond') AT TIME ZONE 'UTC') AT TIME ZONE 'UTC',
                        INTERVAL '1 month'
                    ) month_start
                    WHERE h.valid_from < ?::timestamptz
                      AND COALESCE(h.valid_to, ?::timestamptz) > h.valid_from
                ), weighted AS (
                    SELECT *, EXTRACT(EPOCH FROM (segment_end - segment_start)) AS seconds_in_state
                    FROM segments
                    WHERE segment_end > segment_start
                )
                SELECT offer_id,
                       month,
                       MIN(currency),
                       ROUND(SUM(effective_price * seconds_in_state)
                             FILTER (WHERE effective_price IS NOT NULL)
                             / NULLIF(SUM(seconds_in_state)
                             FILTER (WHERE effective_price IS NOT NULL), 0), 2),
                       MIN(effective_price),
                       MAX(effective_price),
                       (ARRAY_AGG(effective_price ORDER BY segment_start)
                           FILTER (WHERE effective_price IS NOT NULL))[1],
                       (ARRAY_AGG(effective_price ORDER BY segment_start DESC)
                           FILTER (WHERE effective_price IS NOT NULL))[1],
                       ROUND(COALESCE(SUM(seconds_in_state) FILTER (
                           WHERE availability_status IN ('IN_STOCK', 'PREORDER')), 0) / 86400.0)::smallint,
                       ROUND(COALESCE(SUM(seconds_in_state) FILTER (
                           WHERE availability_status = 'OUT_OF_STOCK'), 0) / 86400.0)::smallint,
                       ROUND(COALESCE(SUM(seconds_in_state) FILTER (
                           WHERE availability_status = 'UNKNOWN'), 0) / 86400.0)::smallint,
                       NOW()
                FROM weighted
                GROUP BY offer_id, month
                ON CONFLICT (offer_id, month) DO UPDATE SET
                    currency = EXCLUDED.currency,
                    average_effective_price = EXCLUDED.average_effective_price,
                    minimum_effective_price = EXCLUDED.minimum_effective_price,
                    maximum_effective_price = EXCLUDED.maximum_effective_price,
                    first_effective_price = EXCLUDED.first_effective_price,
                    last_effective_price = EXCLUDED.last_effective_price,
                    in_stock_days = EXCLUDED.in_stock_days,
                    out_of_stock_days = EXCLUDED.out_of_stock_days,
                    unknown_days = EXCLUDED.unknown_days,
                    aggregated_at = EXCLUDED.aggregated_at
                """, timestamp(cutoff), timestamp(cutoff), timestamp(cutoff),
                timestamp(cutoff), timestamp(cutoff));
    }

    /** Строит или обновляет месячную статистику остатков до границы хранения. */
    public int aggregateInventoryMonths(Instant cutoff) {
        return jdbcTemplate.update("""
                INSERT INTO store_inventory_monthly_stats (
                    offer_id, store_location_id, month,
                    in_stock_days, out_of_stock_days, unknown_days, aggregated_at
                )
                WITH segments AS (
                    SELECT h.offer_id,
                           h.store_location_id,
                           (month_start AT TIME ZONE 'UTC')::date AS month,
                           h.availability_status,
                           GREATEST(h.valid_from, month_start) AS segment_start,
                           LEAST(COALESCE(h.valid_to, ?::timestamptz), month_start + INTERVAL '1 month') AS segment_end
                    FROM store_inventory_history h
                    CROSS JOIN LATERAL GENERATE_SERIES(
                        DATE_TRUNC('month', h.valid_from AT TIME ZONE 'UTC') AT TIME ZONE 'UTC',
                        DATE_TRUNC('month', (LEAST(COALESCE(h.valid_to, ?::timestamptz), ?::timestamptz)
                            - INTERVAL '1 microsecond') AT TIME ZONE 'UTC') AT TIME ZONE 'UTC',
                        INTERVAL '1 month'
                    ) month_start
                    WHERE h.valid_from < ?::timestamptz
                      AND COALESCE(h.valid_to, ?::timestamptz) > h.valid_from
                ), weighted AS (
                    SELECT *, EXTRACT(EPOCH FROM (segment_end - segment_start)) AS seconds_in_state
                    FROM segments
                    WHERE segment_end > segment_start
                )
                SELECT offer_id,
                       store_location_id,
                       month,
                       ROUND(COALESCE(SUM(seconds_in_state) FILTER (
                           WHERE availability_status IN ('IN_STOCK', 'PREORDER')), 0) / 86400.0)::smallint,
                       ROUND(COALESCE(SUM(seconds_in_state) FILTER (
                           WHERE availability_status = 'OUT_OF_STOCK'), 0) / 86400.0)::smallint,
                       ROUND(COALESCE(SUM(seconds_in_state) FILTER (
                           WHERE availability_status = 'UNKNOWN'), 0) / 86400.0)::smallint,
                       NOW()
                FROM weighted
                GROUP BY offer_id, store_location_id, month
                ON CONFLICT (offer_id, store_location_id, month) DO UPDATE SET
                    in_stock_days = EXCLUDED.in_stock_days,
                    out_of_stock_days = EXCLUDED.out_of_stock_days,
                    unknown_days = EXCLUDED.unknown_days,
                    aggregated_at = EXCLUDED.aggregated_at
                """, timestamp(cutoff), timestamp(cutoff), timestamp(cutoff),
                timestamp(cutoff), timestamp(cutoff));
    }

    /** Удаляет закрытые интервалы цен, полностью лежащие до границы. */
    public int deleteClosedOfferHistory(Instant cutoff) {
        return jdbcTemplate.update(
                "DELETE FROM offer_state_history WHERE valid_to IS NOT NULL AND valid_to <= ?",
                timestamp(cutoff));
    }

    /** Делит пересекающие границу интервалы цен без разрыва графика. */
    public int splitOfferIntervalsAt(Instant cutoff) {
        return jdbcTemplate.update("""
                WITH spanning AS (
                    SELECT id, valid_to AS original_valid_to
                    FROM offer_state_history
                    WHERE valid_from < ?
                      AND (valid_to IS NULL OR valid_to > ?)
                    FOR UPDATE
                ), closed AS (
                    UPDATE offer_state_history h
                       SET valid_to = ?
                      FROM spanning s
                     WHERE h.id = s.id
                    RETURNING h.offer_id, h.source_run_id, h.regular_price, h.sale_price,
                              h.conditional_price, h.currency, h.availability_status,
                              h.quantity, h.terms_snapshot, h.state_hash, s.original_valid_to
                )
                INSERT INTO offer_state_history (
                    offer_id, source_run_id, regular_price, sale_price, conditional_price,
                    currency, availability_status, quantity, terms_snapshot, state_hash,
                    valid_from, valid_to
                )
                SELECT offer_id, source_run_id, regular_price, sale_price, conditional_price,
                       currency, availability_status, quantity, terms_snapshot, state_hash,
                       ?, original_valid_to
                FROM closed
                """, timestamp(cutoff), timestamp(cutoff), timestamp(cutoff), timestamp(cutoff));
    }

    /** Удаляет закрытые интервалы остатков до границы хранения. */
    public int deleteClosedInventoryHistory(Instant cutoff) {
        return jdbcTemplate.update(
                "DELETE FROM store_inventory_history WHERE valid_to IS NOT NULL AND valid_to <= ?",
                timestamp(cutoff));
    }

    /** Делит пересекающие границу интервалы остатков. */
    public int splitInventoryIntervalsAt(Instant cutoff) {
        return jdbcTemplate.update("""
                WITH spanning AS (
                    SELECT id, valid_to AS original_valid_to
                    FROM store_inventory_history
                    WHERE valid_from < ?
                      AND (valid_to IS NULL OR valid_to > ?)
                    FOR UPDATE
                ), closed AS (
                    UPDATE store_inventory_history h
                       SET valid_to = ?
                      FROM spanning s
                     WHERE h.id = s.id
                    RETURNING h.offer_id, h.store_location_id, h.source_run_id,
                              h.availability_status, h.quantity, h.state_hash, s.original_valid_to
                )
                INSERT INTO store_inventory_history (
                    offer_id, store_location_id, source_run_id, availability_status,
                    quantity, state_hash, valid_from, valid_to
                )
                SELECT offer_id, store_location_id, source_run_id, availability_status,
                       quantity, state_hash, ?, original_valid_to
                FROM closed
                """, timestamp(cutoff), timestamp(cutoff), timestamp(cutoff), timestamp(cutoff));
    }

    /** Преобразует момент времени в JDBC-тип параметра SQL. */
    private static Timestamp timestamp(Instant instant) {
        return Timestamp.from(instant);
    }
}
