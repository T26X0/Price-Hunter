package com.pricehunter.admin;

import com.pricehunter.parser.ReviewStatus;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Решение оператора по неоднозначной позиции и, при слиянии филиалов, выбранная каноническая сущность. */
public record ResolveReviewCandidateRequest(
        @NotNull ReviewStatus decision,
        UUID targetEntityId
) {
}
