package com.pricehunter.parser.http;

import java.net.URI;

/** Минимальный HTTP-контракт парсеров, отделяющий адаптер сайта от сетевой реализации. */
public interface ParserHttpClient {
    /**
     * @param uri публичный HTTPS-адрес источника
     * @return тело успешного ответа как текст
     */
    String get(URI uri);
}
