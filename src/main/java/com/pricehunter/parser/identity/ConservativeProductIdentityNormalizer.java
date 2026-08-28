package com.pricehunter.parser.identity;

import com.pricehunter.parser.connector.ParsedCatalogItem;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
/**
 * Осторожный нормализатор MVP: автоматически объединяет только уверенно распознанные iPhone.
 * Аксессуары, неизвестные модели и неполные конфигурации намеренно отправляет человеку.
 */
public class ConservativeProductIdentityNormalizer implements ProductIdentityNormalizer {

    private static final Pattern IPHONE = Pattern.compile(
            "(?i)\\b(?:Apple\\s+)?iPhone\\s+(\\d{1,2})(?:\\s+(Pro Max|Pro|Plus|mini|Air|E))?");
    private static final Pattern ACCESSORY = Pattern.compile(
            "(?i)\\b(чехол|case|кабель|заряд|стекло|пл[её]нка|ремешок|держатель|адаптер|аксессуар)\\b");

    /** Определяет общую модель iPhone и строит стабильный ключ варианта из памяти, цвета и SIM. */
    @Override
    public NormalizedProductCandidate normalize(ParsedCatalogItem item) {
        if (ACCESSORY.matcher(item.rawName()).find()) {
            return NormalizedProductCandidate.review("Аксессуар или нетехническая позиция требует ручной классификации");
        }

        Matcher iphone = IPHONE.matcher(item.rawName());
        if (!iphone.find()) {
            return NormalizedProductCandidate.review("Не удалось надёжно определить модель технического товара");
        }

        String suffix = iphone.group(2) == null ? "" : " " + titleCase(iphone.group(2));
        String modelName = "iPhone " + iphone.group(1) + suffix;
        String brand = item.brand() == null ? "Apple" : item.brand();
        String catalogKey = slug(brand + " " + modelName);

        Map<String, String> normalizedAttributes = item.attributes().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> normalizeValue(entry.getValue()),
                        (left, right) -> left,
                        LinkedHashMap::new));

        if (!normalizedAttributes.containsKey("storage") || !normalizedAttributes.containsKey("color")) {
            return NormalizedProductCandidate.review("У конфигурации iPhone не определены память или цвет");
        }

        String variantKey = normalizedAttributes.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> entry.getKey() + "=" + slug(entry.getValue()))
                .collect(Collectors.joining("|"));

        return new NormalizedProductCandidate(
                true,
                null,
                brand,
                modelName,
                catalogKey,
                "smartphones",
                variantKey,
                item.rawName(),
                normalizedAttributes
        );
    }

    /** Превращает отображаемое значение в стабильный ключ для поиска дублей. */
    static String slug(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replace('ё', 'е')
                .replaceAll("[^a-zа-я0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    /** Нормализует регистр, пробелы и букву «ё» в значении характеристики. */
    private static String normalizeValue(String value) {
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT).replace('ё', 'е');
    }

    /** Восстанавливает принятую запись модификатора модели iPhone. */
    private static String titleCase(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "pro max" -> "Pro Max";
            case "pro" -> "Pro";
            case "plus" -> "Plus";
            case "mini" -> "mini";
            case "air" -> "Air";
            case "e" -> "E";
            default -> value;
        };
    }
}
