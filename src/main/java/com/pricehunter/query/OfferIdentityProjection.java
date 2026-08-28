package com.pricehunter.query;

import java.time.Instant;
import java.util.UUID;

/** Минимальные поля предложения для пакетной дедупликации парсером. */
public interface OfferIdentityProjection {
    /** @return внутренний ID предложения */
    UUID getId();
    /** @return ID предложения во внешнем источнике */
    String getExternalOfferId();
    /** @return запасной канонический ключ предложения */
    String getOfferKey();
    /** @return отпечаток текущего состояния */
    String getStateHash();
    /** @return время последней проверки */
    Instant getLastCheckedAt();
}
