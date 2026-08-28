package com.pricehunter.parser.http;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class RobotsRulesTest {

    private final RobotsRules rules = RobotsRules.parse("""
            User-agent: *
            Allow: /catalog
            Allow: /product/
            Disallow: /api/
            Disallow: /*?*
            Disallow: /catalog?*
            Crawl-delay: 1
            """);

    @Test
    void allowsCatalogAndProductPages() {
        assertThat(rules.allows(URI.create("https://941store.ru/catalog/iphone-16-pro"))).isTrue();
        assertThat(rules.allows(URI.create("https://941store.ru/product/iphone"))).isTrue();
    }

    @Test
    void blocksApiAndFilteredUrls() {
        assertThat(rules.allows(URI.create("https://941store.ru/api/products"))).isFalse();
        assertThat(rules.allows(URI.create("https://941store.ru/catalog?color=black"))).isFalse();
    }
}
