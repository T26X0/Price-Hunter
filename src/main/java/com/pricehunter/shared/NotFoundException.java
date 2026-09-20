package com.pricehunter.shared;

/** Сигнализирует, что запрошенный публичный ресурс отсутствует. */
public class NotFoundException extends RuntimeException {

    /** Создаёт ошибку с безопасным для API сообщением. */
    public NotFoundException(String message) {
        super(message);
    }
}
