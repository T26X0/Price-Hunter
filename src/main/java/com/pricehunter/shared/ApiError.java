package com.pricehunter.shared;

import java.time.Instant;
import java.util.Map;

/** Единый формат ошибки REST API, включая ошибки конкретных полей. */
public record ApiError(Instant timestamp, int status, String error, String message, Map<String, String> fields) {
}
