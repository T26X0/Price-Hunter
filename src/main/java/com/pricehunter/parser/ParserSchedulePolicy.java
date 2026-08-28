package com.pricehunter.parser;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
/** Единое место, где заданы интервалы обновления цен, товаров и магазинов. */
public class ParserSchedulePolicy {

    public static final Duration PRICE_INTERVAL = Duration.ofDays(1);
    public static final Duration PRODUCT_INTERVAL = Duration.ofDays(7);
    public static final Duration STORE_INTERVAL = Duration.ofDays(14);

    /**
     * Вычисляет границу устаревания: источник с последним запуском раньше неё считается готовым.
     */
    public Instant cutoff(ParserJobType jobType, Instant now) {
        return switch (jobType) {
            case PRICE_REFRESH -> now.minus(PRICE_INTERVAL);
            case PRODUCT_DISCOVERY -> now.minus(PRODUCT_INTERVAL);
            case STORE_DISCOVERY -> now.minus(STORE_INTERVAL);
        };
    }
}
