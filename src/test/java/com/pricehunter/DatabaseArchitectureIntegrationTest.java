package com.pricehunter;

import com.pricehunter.city.City;
import com.pricehunter.city.CityRepository;
import com.pricehunter.history.HistoryRetentionService;
import com.pricehunter.history.OfferStateHistoryRepository;
import com.pricehunter.offer.AvailabilityStatus;
import com.pricehunter.offer.ConditionType;
import com.pricehunter.offer.OfferIngestionService;
import com.pricehunter.offer.OfferRepository;
import com.pricehunter.offer.OfferSnapshot;
import com.pricehunter.product.Product;
import com.pricehunter.product.ProductRepository;
import com.pricehunter.product.ProductVariant;
import com.pricehunter.product.ProductVariantRepository;
import com.pricehunter.retail.ChainCityMarket;
import com.pricehunter.retail.ChainCityMarketRepository;
import com.pricehunter.retail.RetailChain;
import com.pricehunter.retail.RetailChainRepository;
import com.pricehunter.retail.SalesChannel;
import com.pricehunter.store.ParserType;
import com.pricehunter.store.Store;
import com.pricehunter.store.StoreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnabledIfEnvironmentVariable(named = "RUN_DB_INTEGRATION_TESTS", matches = "true")
class DatabaseArchitectureIntegrationTest {

    @Autowired CityRepository cityRepository;
    @Autowired RetailChainRepository chainRepository;
    @Autowired ChainCityMarketRepository marketRepository;
    @Autowired ProductRepository productRepository;
    @Autowired ProductVariantRepository variantRepository;
    @Autowired OfferRepository offerRepository;
    @Autowired OfferStateHistoryRepository historyRepository;
    @Autowired OfferIngestionService ingestionService;
    @Autowired HistoryRetentionService retentionService;
    @Autowired StoreRepository storeRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    @Transactional
    void storesOneCityPriceAsChangeOnlyHistoryAndUsesIndexedProjections() {
        City city = cityRepository.save(new City("Екатеринбург", "RU", "Asia/Yekaterinburg"));
        RetailChain chain = chainRepository.save(
                new RetailChain("941", "941", "https://941.example", ParserType.NINE41, true));
        ChainCityMarket market = marketRepository.save(
                new ChainCityMarket(chain, city, SalesChannel.PHYSICAL, "ekb", "https://941.example/ekb", true));
        Product product = productRepository.save(
                new Product("Apple", "iPhone 16 Pro", "IPHONE-16-PRO", "smartphones", null));
        ProductVariant variant = variantRepository.save(
                new ProductVariant(product, "storage=256gb;color=black", "A3294-256-BLK",
                        "256 ГБ, Black"));

        Instant firstDay = Instant.parse("2025-01-01T00:00:00Z");
        var created = ingestionService.ingest(snapshot(market, variant, firstDay, "115000"), null);
        var unchanged = ingestionService.ingest(snapshot(market, variant,
                Instant.parse("2025-01-02T00:00:00Z"), "115000"), null);
        var changed = ingestionService.ingest(snapshot(market, variant,
                Instant.parse("2025-02-01T00:00:00Z"), "110000"), null);

        assertThat(created.created()).isTrue();
        assertThat(unchanged.stateChanged()).isFalse();
        assertThat(changed.stateChanged()).isTrue();
        assertThat(historyRepository.findChartRange(created.offerId(), firstDay,
                Instant.parse("2025-04-01T00:00:00Z"))).hasSize(2);

        assertThat(marketRepository.findParserTargets(ParserType.NINE41, PageRequest.of(0, 20)))
                .hasSize(1);
        assertThat(offerRepository.findBestLocalOffers(variant.getId(), city.getId(), PageRequest.of(0, 20)))
                .hasSize(1);
        assertThat(offerRepository.findCard(created.offerId())).isPresent();

        Instant cutoff = Instant.parse("2025-04-01T00:00:00Z");
        var retention = retentionService.aggregateAndPrune(cutoff);
        assertThat(retention.aggregatedOfferMonths()).isEqualTo(3);
        assertThat(retention.anchoredOfferIntervals()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from offer_state_history where offer_id = ?",
                Long.class, created.offerId())).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from offer_monthly_stats where offer_id = ?",
                Long.class, created.offerId())).isEqualTo(3L);

        Store radishcheva = storeRepository.save(new Store(
                chain, market, "941-radishcheva", "941 Радищева", "ул. Радищева, 1", "https://941.example"));
        Store malysheva = storeRepository.save(new Store(
                chain, market, "941-malysheva", "941 Малышева", "ул. Малышева, 73", "https://941.example"));
        Instant branchCheck = Instant.parse("2025-06-01T00:00:00Z");
        ingestionService.ingest(snapshot(market, radishcheva, variant, branchCheck, "105000"), null);
        ingestionService.ingest(snapshot(market, malysheva, variant, branchCheck, "108000"), null);

        var localOffers = offerRepository.findBestLocalOffers(
                variant.getId(), city.getId(), PageRequest.of(0, 20)).getContent();
        assertThat(localOffers).hasSize(3);
        assertThat(localOffers.getFirst().getStoreLocationId()).isEqualTo(radishcheva.getId());
        assertThat(localOffers.getFirst().getStoreLocationName()).isEqualTo("941 Радищева");
    }

    private static OfferSnapshot snapshot(ChainCityMarket market, ProductVariant variant,
                                          Instant observedAt, String price) {
        return snapshot(market, null, variant, observedAt, price);
    }

    private static OfferSnapshot snapshot(ChainCityMarket market, Store store, ProductVariant variant,
                                          Instant observedAt, String price) {
        return new OfferSnapshot(
                market.getId(), store == null ? null : store.getId(), variant.getId(),
                "941-iphone-16-pro-256-black-new",
                "variant:new", ConditionType.NEW, new BigDecimal(price), null, null,
                "RUB", AvailabilityStatus.IN_STOCK, 10,
                "https://941.example/ekb/iphone-16-pro-256-black",
                observedAt, observedAt.plusSeconds(172800), List.of());
    }
}
