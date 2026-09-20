package com.pricehunter.offer;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** Краткое публичное представление филиала, к которому привязана цена или остаток. */
@Schema(description = "Филиал магазина")
public record StoreResponse(UUID id, String name, String address) {
}
