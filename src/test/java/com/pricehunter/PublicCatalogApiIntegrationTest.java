package com.pricehunter;

import com.pricehunter.admin.AdminReviewService;
import com.pricehunter.admin.ResolveReviewCandidateRequest;
import com.pricehunter.city.City;
import com.pricehunter.city.CityRepository;
import com.pricehunter.offer.AvailabilityStatus;
import com.pricehunter.offer.ConditionType;
import com.pricehunter.offer.InventorySnapshot;
import com.pricehunter.offer.OfferIngestionService;
import com.pricehunter.offer.OfferSnapshot;
import com.pricehunter.offer.OfferTermRepository;
import com.pricehunter.offer.OfferQueryService;
import com.pricehunter.offer.StoreInventoryIngestionService;
import com.pricehunter.parser.ConnectorMode;
import com.pricehunter.parser.ParserSource;
import com.pricehunter.parser.ParserSourceRepository;
import com.pricehunter.parser.ParserSourceType;
import com.pricehunter.parser.ReviewCandidate;
import com.pricehunter.parser.ReviewCandidateRepository;
import com.pricehunter.parser.ReviewStatus;
import com.pricehunter.parser.ReviewType;
import com.pricehunter.parser.SourceEntityLinkRepository;
import com.pricehunter.product.AttributeDataType;
import com.pricehunter.product.AttributeDefinition;
import com.pricehunter.product.AttributeDefinitionRepository;
import com.pricehunter.product.CatalogQueryService;
import com.pricehunter.product.Product;
import com.pricehunter.product.ProductRepository;
import com.pricehunter.product.ProductVariant;
import com.pricehunter.product.ProductVariantAttribute;
import com.pricehunter.product.ProductVariantAttributeRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Проверяет Public Catalog API на настоящей PostgreSQL без HTTP-сервера и без mock-репозиториев. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnabledIfEnvironmentVariable(named = "RUN_DB_INTEGRATION_TESTS", matches = "true")
@Transactional
class PublicCatalogApiIntegrationTest {

    @Autowired CityRepository cityRepository;
    @Autowired RetailChainRepository chainRepository;
    @Autowired ChainCityMarketRepository marketRepository;
    @Autowired StoreRepository storeRepository;
    @Autowired ProductRepository productRepository;
    @Autowired ProductVariantRepository variantRepository;
    @Autowired AttributeDefinitionRepository attributeRepository;
    @Autowired ProductVariantAttributeRepository variantAttributeRepository;
    @Autowired OfferIngestionService offerIngestionService;
    @Autowired StoreInventoryIngestionService inventoryIngestionService;
    @Autowired OfferTermRepository offerTermRepository;
    @Autowired CatalogQueryService catalogQueryService;
    @Autowired OfferQueryService offerQueryService;
    @Autowired ParserSourceRepository sourceRepository;
    @Autowired ReviewCandidateRepository reviewRepository;
    @Autowired SourceEntityLinkRepository linkRepository;
    @Autowired AdminReviewService reviewService;

    /** Создаёт один вертикальный срез и проверяет каталог, карточку, офферы, историю, условия и остаток. */
    @Test
    void exposesCatalogOfferHistoryTermsAndInventoryWithoutLeakingEntities() {
        City city = cityRepository.save(new City("Екатеринбург", "RU", "Asia/Yekaterinburg"));
        RetailChain chain = chainRepository.save(
                new RetailChain("941", "941", "https://941store.ru", ParserType.NINE41, true));
        ChainCityMarket market = marketRepository.save(new ChainCityMarket(
                chain, city, SalesChannel.PHYSICAL, "ekaterinburg", "https://941store.ru", true));
        Store store = storeRepository.save(new Store(
                chain, market, "941-ekb-1", "941 Екатеринбург", "ул. Малышева, 1", "https://941store.ru"));
        Product product = productRepository.save(new Product(
                "Apple", "iPhone 16 Pro", "APPLE-IPHONE-16-PRO", "smartphones", "Тестовая карточка"));
        ProductVariant variant = variantRepository.save(new ProductVariant(
                product, "storage=256 gb;color=black", null, "256 ГБ, Black"));
        AttributeDefinition storage = attributeRepository.save(new AttributeDefinition(
                "storage", "Объём памяти", AttributeDataType.NUMBER, "GB", true));
        variantAttributeRepository.save(new ProductVariantAttribute(
                variant, storage, "256 GB", "256 ГБ", new BigDecimal("256"), null));

        Instant observedAt = Instant.parse("2026-09-20T00:00:00Z");
        var upsert = offerIngestionService.ingest(new OfferSnapshot(
                market.getId(), store.getId(), variant.getId(), "941-iphone-16-pro-256-black",
                "941:iphone-16-pro-256-black", ConditionType.NEW, new BigDecimal("119000"),
                new BigDecimal("115000"), null, "RUB", AvailabilityStatus.IN_STOCK, 5,
                "https://941store.ru/product/iphone-16-pro-256-black", observedAt, observedAt.plusSeconds(172800),
                List.of(Map.of("type", "GIFT", "title", "Чехол в подарок"))), null);
        inventoryIngestionService.ingest(new InventorySnapshot(
                upsert.offerId(), store.getId(), AvailabilityStatus.IN_STOCK, 5, observedAt), null);

        var catalog = catalogQueryService.findProducts("iPhone", city.getId(), "smartphones", "Apple", 0, 20);
        assertThat(catalog.items()).hasSize(1);
        assertThat(catalog.items().getFirst().minEffectivePrice()).isEqualByComparingTo("115000");

        var detail = catalogQueryService.findProduct("APPLE-IPHONE-16-PRO", city.getId());
        assertThat(detail.variants()).hasSize(1);
        assertThat(detail.variants().getFirst().attributes()).extracting(attribute -> attribute.code())
                .containsExactly("storage");
        assertThat(detail.variants().getFirst().available()).isTrue();

        var offers = offerQueryService.findVariantOffers(variant.getId(), city.getId(), 0, 20);
        assertThat(offers.items()).hasSize(1);
        assertThat(offers.items().getFirst().effectivePrice()).isEqualByComparingTo("115000");

        var offer = offerQueryService.findOffer(upsert.offerId());
        assertThat(offer.terms()).extracting(term -> term.title()).containsExactly("Чехол в подарок");
        assertThat(offerQueryService.findHistory(upsert.offerId(), observedAt.minusSeconds(1), observedAt.plusSeconds(1)))
                .hasSize(1);
        assertThat(offerQueryService.findInventory(upsert.offerId(), 0, 20).items())
                .extracting(inventory -> inventory.storeName())
                .containsExactly("941 Екатеринбург");
        assertThat(offerQueryService.findDeliveredOffers(variant.getId(), city.getId(), 0, 20).items()).isEmpty();
        assertThat(offerTermRepository.findActiveTerms(upsert.offerId(), observedAt)).hasSize(1);
    }

    /** Проверяет, что слияние филиала вручную создаёт используемую импортом source link, а не только меняет статус. */
    @Test
    void mergesStoreReviewCandidateIntoARealSourceLink() {
        City city = cityRepository.save(new City("Челябинск", "RU", "Asia/Yekaterinburg"));
        RetailChain chain = chainRepository.save(
                new RetailChain("941", "941-chel", "https://941store.ru", ParserType.NINE41, true));
        ChainCityMarket market = marketRepository.save(new ChainCityMarket(
                chain, city, SalesChannel.PHYSICAL, "chelyabinsk", "https://941store.ru", true));
        Store store = storeRepository.save(new Store(
                chain, market, "941-chel-1", "941 Челябинск", "ул. Кирова, 1", "https://941store.ru"));
        ParserSource source = sourceRepository.save(new ParserSource(
                market, ParserSourceType.WEBSITE, ConnectorMode.HTML, "941", "941 Челябинск",
                "https://941store.ru/catalog/iphone-16-pro", Map.of("cityKey", "chelyabinsk")));
        ReviewCandidate candidate = reviewRepository.save(new ReviewCandidate(
                source, null, ReviewType.STORE_MATCH, "fixture-store-match", "941 Челябинск",
                null, "https://941store.ru/store/chelyabinsk-1", "external-chel-1", null, null,
                "Филиал требует сопоставления", Map.of(), Instant.parse("2026-09-20T00:00:00Z")));

        assertThat(reviewService.findCandidates(ReviewStatus.PENDING, 0, 20).items())
                .extracting(review -> review.id()).contains(candidate.getId());

        var resolved = reviewService.resolve(candidate.getId(),
                new ResolveReviewCandidateRequest(ReviewStatus.MERGED, store.getId()));

        assertThat(resolved.status()).isEqualTo(ReviewStatus.MERGED);
        assertThat(resolved.suggestedEntityId()).isEqualTo(store.getId());
        assertThat(linkRepository.findByParserSourceIdAndEntityTypeAndExternalId(
                source.getId(), com.pricehunter.parser.SourceEntityType.STORE_LOCATION, "external-chel-1"))
                .get().extracting(link -> link.getInternalEntityId()).isEqualTo(store.getId());
    }
}
