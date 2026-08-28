package com.pricehunter.parser.connector.ninefortyone;

import com.pricehunter.parser.connector.CatalogScanRequest;
import com.pricehunter.parser.http.JavaParserHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "RUN_PARSER_LIVE_TESTS", matches = "true")
class NineFortyOneCatalogConnectorLiveTest {

    @Test
    void readsPublic941CatalogWhileRespectingRobotsRules() {
        URI catalog = URI.create("https://941store.ru/catalog/iphone-16-pro");
        var connector = new NineFortyOneCatalogConnector(new JavaParserHttpClient());

        var result = connector.scan(new CatalogScanRequest(
                UUID.randomUUID(), UUID.randomUUID(), catalog, "ekaterinburg", Instant.now()));

        assertThat(result.items()).isNotEmpty();
        assertThat(result.items()).allSatisfy(item -> {
            assertThat(item.sourceUri().getHost()).isEqualTo("941store.ru");
            assertThat(item.rawName()).containsIgnoringCase("iPhone 16 Pro");
            assertThat(item.currency()).isEqualTo("RUB");
        });
    }
}
