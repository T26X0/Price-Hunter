package com.pricehunter;

import com.pricehunter.city.City;
import com.pricehunter.city.CityRepository;
import com.pricehunter.history.OfferStateHistoryRepository;
import com.pricehunter.offer.CollectionRun;
import com.pricehunter.offer.CollectionRunRepository;
import com.pricehunter.offer.OfferRepository;
import com.pricehunter.parser.CatalogPersistenceService;
import com.pricehunter.parser.ConnectorMode;
import com.pricehunter.parser.ParserJob;
import com.pricehunter.parser.ParserJobRepository;
import com.pricehunter.parser.ParserJobType;
import com.pricehunter.parser.ParserSource;
import com.pricehunter.parser.ParserSourceRepository;
import com.pricehunter.parser.ParserSourceType;
import com.pricehunter.parser.ReviewCandidateRepository;
import com.pricehunter.parser.SourceEntityLinkRepository;
import com.pricehunter.parser.connector.CatalogScanResult;
import com.pricehunter.parser.connector.ninefortyone.NineFortyOneCatalogConnector;
import com.pricehunter.product.ProductRepository;
import com.pricehunter.product.ProductVariantAttributeRepository;
import com.pricehunter.product.ProductVariantRepository;
import com.pricehunter.retail.ChainCityMarket;
import com.pricehunter.retail.ChainCityMarketRepository;
import com.pricehunter.retail.RetailChain;
import com.pricehunter.retail.RetailChainRepository;
import com.pricehunter.retail.SalesChannel;
import com.pricehunter.store.ParserType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnabledIfEnvironmentVariable(named = "RUN_DB_INTEGRATION_TESTS", matches = "true")
class ParserPipelineIntegrationTest {

    @Autowired CityRepository cityRepository;
    @Autowired RetailChainRepository chainRepository;
    @Autowired ChainCityMarketRepository marketRepository;
    @Autowired ParserSourceRepository sourceRepository;
    @Autowired ParserJobRepository jobRepository;
    @Autowired CollectionRunRepository collectionRunRepository;
    @Autowired CatalogPersistenceService persistenceService;
    @Autowired NineFortyOneCatalogConnector connector;
    @Autowired ProductRepository productRepository;
    @Autowired ProductVariantRepository variantRepository;
    @Autowired ProductVariantAttributeRepository variantAttributeRepository;
    @Autowired OfferRepository offerRepository;
    @Autowired OfferStateHistoryRepository historyRepository;
    @Autowired ReviewCandidateRepository reviewRepository;
    @Autowired SourceEntityLinkRepository linkRepository;

    @Test
    @Transactional
    void imports941FixtureWithoutDuplicatingModelsVariantsOffersOrHistory() throws IOException {
        City city = cityRepository.save(new City("Екатеринбург", "RU", "Asia/Yekaterinburg"));
        RetailChain chain = chainRepository.save(
                new RetailChain("941", "941", "https://941store.ru", ParserType.NINE41, true));
        ChainCityMarket market = marketRepository.save(new ChainCityMarket(
                chain, city, SalesChannel.PHYSICAL, "ekaterinburg", "https://941store.ru", true));
        ParserSource source = sourceRepository.save(new ParserSource(
                market, ParserSourceType.WEBSITE, ConnectorMode.HTML, "941", "941 Екатеринбург",
                "https://941store.ru/catalog/iphone-16-pro", Map.of("cityKey", "ekaterinburg")));

        Instant first = Instant.parse("2026-08-28T00:00:00Z");
        CatalogScanResult firstScan = connector.parse(fixture(), URI.create(source.getBaseUrl()), first);
        ParserJob firstJob = runningJob(source, market, first);
        var firstOutcome = persistenceService.persist(source.getId(), firstJob.getId(), firstScan);

        assertThat(firstOutcome.foundCount()).isEqualTo(2);
        assertThat(firstOutcome.createdCount()).isEqualTo(2);
        assertThat(firstOutcome.changedCount()).isEqualTo(2);
        assertThat(firstOutcome.reviewCount()).isZero();
        assertThat(productRepository.count()).isEqualTo(1);
        assertThat(variantRepository.count()).isEqualTo(2);
        assertThat(variantAttributeRepository.count()).isEqualTo(6);
        assertThat(offerRepository.count()).isEqualTo(2);
        assertThat(historyRepository.count()).isEqualTo(2);
        assertThat(linkRepository.count()).isEqualTo(5);

        Instant second = first.plusSeconds(86400);
        CatalogScanResult secondScan = connector.parse(fixture(), URI.create(source.getBaseUrl()), second);
        ParserJob secondJob = runningJob(source, market, second);
        var secondOutcome = persistenceService.persist(source.getId(), secondJob.getId(), secondScan);

        assertThat(secondOutcome.createdCount()).isZero();
        assertThat(secondOutcome.changedCount()).isZero();
        assertThat(productRepository.count()).isEqualTo(1);
        assertThat(variantRepository.count()).isEqualTo(2);
        assertThat(offerRepository.count()).isEqualTo(2);
        assertThat(historyRepository.count()).isEqualTo(2);
        assertThat(reviewRepository.count()).isZero();
    }

    private ParserJob runningJob(ParserSource source, ChainCityMarket market, Instant startedAt) {
        ParserJob job = jobRepository.save(new ParserJob(source, ParserJobType.PRICE_REFRESH, startedAt));
        job.start(startedAt);
        CollectionRun run = collectionRunRepository.save(new CollectionRun(market, startedAt));
        job.attachCollectionRun(run);
        return job;
    }

    private String fixture() throws IOException {
        try (var stream = getClass().getResourceAsStream("/parser/941-iphone16pro.html")) {
            if (stream == null) {
                throw new IllegalStateException("Parser fixture is missing");
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
