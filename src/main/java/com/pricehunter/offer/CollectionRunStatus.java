package com.pricehunter.offer;

/** Итоговый статус одного общего прохода сбора данных по рынку. */
public enum CollectionRunStatus {
    RUNNING,
    SUCCEEDED,
    PARTIALLY_SUCCEEDED,
    FAILED
}
