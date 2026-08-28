package com.pricehunter.parser.connector.ninefortyone;

import com.pricehunter.offer.AvailabilityStatus;
import com.pricehunter.parser.connector.CatalogScanResult;
import com.pricehunter.parser.connector.ParsedCatalogItem;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class NineFortyOneCatalogConnectorTest {

    private final NineFortyOneCatalogConnector connector = new NineFortyOneCatalogConnector(
            uri -> { throw new UnsupportedOperationException("Network is not used in fixture tests"); });

    @Test
    void parsesStructuredPricesAttributesAndTermsWithoutCssHashes() throws IOException {
        String html;
        try (var stream = getClass().getResourceAsStream("/parser/941-iphone16pro.html")) {
            if (stream == null) {
                throw new IllegalStateException("Parser fixture is missing");
            }
            html = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        CatalogScanResult result = connector.parse(
                html,
                URI.create("https://941store.ru/catalog/iphone-16-pro"),
                Instant.parse("2026-08-28T00:00:00Z"));

        assertThat(result.items()).hasSize(2);
        ParsedCatalogItem discounted = result.items().getFirst();
        assertThat(discounted.externalId())
                .isEqualTo("apple-iphone-16-pro-128gb-esim-desert-titanium-pustynnyj-titan");
        assertThat(discounted.regularPrice()).isEqualByComparingTo("107890");
        assertThat(discounted.salePrice()).isEqualByComparingTo("80390");
        assertThat(discounted.attributes()).containsEntry("storage", "128 GB")
                .containsEntry("sim", "eSIM")
                .containsEntry("color", "пустынный титан");
        assertThat(discounted.terms()).extracting(term -> term.get("type"))
                .containsExactlyInAnyOrder("INSTALLMENT", "DISCOUNT_CONDITION", "GIFT");

        ParsedCatalogItem unavailable = result.items().get(1);
        assertThat(unavailable.regularPrice()).isEqualByComparingTo("105990");
        assertThat(unavailable.salePrice()).isNull();
        assertThat(unavailable.availabilityStatus()).isEqualTo(AvailabilityStatus.OUT_OF_STOCK);
    }
}
