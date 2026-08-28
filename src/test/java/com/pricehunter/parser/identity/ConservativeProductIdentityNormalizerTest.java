package com.pricehunter.parser.identity;

import com.pricehunter.offer.AvailabilityStatus;
import com.pricehunter.offer.ConditionType;
import com.pricehunter.parser.connector.ParsedCatalogItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConservativeProductIdentityNormalizerTest {

    private final ConservativeProductIdentityNormalizer normalizer =
            new ConservativeProductIdentityNormalizer();

    @Test
    void normalizesIphoneModelAndVariantWithoutCreatingCityDuplicates() {
        NormalizedProductCandidate result = normalizer.normalize(item(
                "Apple iPhone 16 Pro 256Gb eSIM Black Titanium, титановый черный",
                Map.of("storage", "256 GB", "sim", "eSIM", "color", "титановый черный")));

        assertThat(result.automaticImportAllowed()).isTrue();
        assertThat(result.modelName()).isEqualTo("iPhone 16 Pro");
        assertThat(result.catalogKey()).isEqualTo("apple-iphone-16-pro");
        assertThat(result.variantKey()).isEqualTo(
                "color=титановый-черный|sim=esim|storage=256-gb");
    }

    @Test
    void sendsCasesToManualReview() {
        NormalizedProductCandidate result = normalizer.normalize(item(
                "Чехол Silicone Case для Apple iPhone 16 Pro Black, черный",
                Map.of("color", "черный")));

        assertThat(result.automaticImportAllowed()).isFalse();
        assertThat(result.reviewReason()).contains("ручной классификации");
    }

    private static ParsedCatalogItem item(String name, Map<String, String> attributes) {
        return new ParsedCatalogItem(
                "external", null, URI.create("https://941store.ru/product/external"),
                name, "Apple", "smartphones", ConditionType.NEW,
                new BigDecimal("100000"), null, null, "RUB",
                AvailabilityStatus.IN_STOCK, null, attributes, java.util.List.of(), Map.of());
    }
}
