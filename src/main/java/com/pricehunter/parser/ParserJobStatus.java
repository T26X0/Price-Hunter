package com.pricehunter.parser;

/** Текущее состояние задания парсера от постановки в очередь до окончательного результата. */
public enum ParserJobStatus {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    PARTIALLY_SUCCEEDED,
    FAILED,
    NEEDS_REVIEW
}
