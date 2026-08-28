package com.pricehunter.parser;

/** Состояние решения по кандидату, отправленному оператору на ручную проверку. */
public enum ReviewStatus {
    PENDING,
    APPROVED,
    MERGED,
    REJECTED
}
