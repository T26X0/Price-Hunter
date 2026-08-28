package com.pricehunter.parser.identity;

import java.util.Map;

/**
 * Результат распознавания товара: единая модель, ключ конфигурации и нормализованные характеристики.
 * Если {@code automaticImportAllowed=false}, остальные идентификационные поля могут быть пустыми.
 */
public record NormalizedProductCandidate(
        boolean automaticImportAllowed,
        String reviewReason,
        String brand,
        String modelName,
        String catalogKey,
        String categoryCode,
        String variantKey,
        String variantDisplayName,
        Map<String, String> attributes
) {
    /** Копирует атрибуты в неизменяемую карту. */
    public NormalizedProductCandidate {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    /**
     * Создаёт результат, который нельзя импортировать без решения оператора.
     *
     * @param reason понятная человеку причина остановки
     * @return кандидат для очереди ручной проверки
     */
    public static NormalizedProductCandidate review(String reason) {
        return new NormalizedProductCandidate(false, reason, null, null, null, null, null, null, Map.of());
    }
}
