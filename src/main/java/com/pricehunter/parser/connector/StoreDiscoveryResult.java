package com.pricehunter.parser.connector;

import java.time.Instant;
import java.util.List;

/** Результат одного прохода по поисковику, Яндекс Картам или 2ГИС. */
public record StoreDiscoveryResult(Instant fetchedAt, List<DiscoveredStore> stores) {
    /** Защищает результат от последующего изменения списка вызывающим кодом. */
    public StoreDiscoveryResult {
        stores = stores == null ? List.of() : List.copyOf(stores);
    }
}
