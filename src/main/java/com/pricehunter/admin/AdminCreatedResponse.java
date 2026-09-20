package com.pricehunter.admin;

import java.util.UUID;

/** Минимальный ответ успешного создания административного ресурса. */
public record AdminCreatedResponse(UUID id) {
}
