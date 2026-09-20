package com.pricehunter.offer;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Публичное коммерческое условие предложения: скидка, подарок, рассрочка или кредит. */
@Schema(description = "Дополнительное условие покупки")
public record OfferTermResponse(
        UUID id,
        OfferTermType type,
        String title,
        String description,
        BigDecimal monetaryValue,
        BigDecimal annualRate,
        Short termMonths,
        BigDecimal monthlyPayment,
        BigDecimal initialPayment,
        Instant validUntil,
        String termsUrl
) {

    /** Преобразует внутреннее коммерческое условие в DTO без ссылки на JPA offer. */
    static OfferTermResponse from(OfferTerm term) {
        return new OfferTermResponse(term.getId(), term.getTermType(), term.getTitle(), term.getDescription(),
                term.getMonetaryValue(), term.getAnnualRate(), term.getTermMonths(), term.getMonthlyPayment(),
                term.getInitialPayment(), term.getValidUntil(), term.getTermsUrl());
    }
}
