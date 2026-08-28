package com.pricehunter.shared;

/** Бизнес-конфликт запроса, например попытка создать товар с занятым SKU. */
public class ConflictException extends RuntimeException {
    /** Создаёт конфликт с безопасным сообщением для клиента. */
    public ConflictException(String message) {
        super(message);
    }
}
