package com.pricehunter.parser.http;

/** Ошибка безопасного получения внешней страницы: сеть, HTTP-статус, лимит или robots.txt. */
public class ParserHttpException extends RuntimeException {
    /** Создаёт ошибку с сообщением для журнала задания. */
    public ParserHttpException(String message) {
        super(message);
    }

    /** Создаёт ошибку с исходной технической причиной. */
    public ParserHttpException(String message, Throwable cause) {
        super(message, cause);
    }
}
