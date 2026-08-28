package com.pricehunter.parser;

import com.pricehunter.offer.OfferIngestionService;
import com.pricehunter.offer.OfferSnapshot;
import com.pricehunter.offer.OfferUpsertResult;
import com.pricehunter.parser.connector.CatalogScanResult;
import com.pricehunter.parser.connector.ParsedCatalogItem;
import com.pricehunter.parser.identity.FingerprintService;
import com.pricehunter.parser.identity.NormalizedProductCandidate;
import com.pricehunter.parser.identity.ProductIdentityNormalizer;
import com.pricehunter.product.AttributeDataType;
import com.pricehunter.product.AttributeDefinition;
import com.pricehunter.product.AttributeDefinitionRepository;
import com.pricehunter.product.Product;
import com.pricehunter.product.ProductRepository;
import com.pricehunter.product.ProductVariant;
import com.pricehunter.product.ProductVariantAttribute;
import com.pricehunter.product.ProductVariantAttributeRepository;
import com.pricehunter.product.ProductVariantRepository;
import com.pricehunter.store.Store;
import com.pricehunter.store.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
/**
 * Транзакционный центр импорта каталога.
 * Нормализует позиции, предотвращает дубли, создаёт модели и конфигурации, обновляет предложения
 * и перенаправляет неоднозначные случаи в очередь ручной проверки.
 */
public class CatalogPersistenceService {

    private final ParserSourceRepository sourceRepository;
    private final ParserJobRepository jobRepository;
    private final ReviewCandidateRepository reviewRepository;
    private final SourceEntityLinkRepository linkRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final AttributeDefinitionRepository attributeRepository;
    private final ProductVariantAttributeRepository variantAttributeRepository;
    private final StoreRepository storeRepository;
    private final ProductIdentityNormalizer identityNormalizer;
    private final FingerprintService fingerprintService;
    private final OfferIngestionService offerIngestionService;

    /**
     * Сохраняет все позиции снимка независимо: ошибка одной позиции не отменяет успешные соседние позиции.
     *
     * @return статистика созданных, изменённых и отправленных на проверку записей
     */
    @Transactional
    public ParserJobOutcome persist(UUID sourceId, UUID jobId, CatalogScanResult scan) {
        ParserSource source = sourceRepository.findById(sourceId).orElseThrow();
        ParserJob job = jobRepository.findById(jobId).orElseThrow();
        if (source.getMarket() == null) {
            throw new IllegalStateException("Website parser source must be assigned to a market");
        }

        int created = 0;
        int changed = 0;
        int review = 0;
        int errors = 0;

        for (ParsedCatalogItem item : scan.items()) {
            try {
                NormalizedProductCandidate normalized = identityNormalizer.normalize(item);
                if (!normalized.automaticImportAllowed()) {
                    upsertReview(source, job, item, ReviewType.CATEGORY_CLASSIFICATION,
                            normalized.reviewReason(), scan.fetchedAt());
                    review++;
                    continue;
                }

                UUID storeLocationId = resolveStore(source, job, item, scan.fetchedAt());
                if (item.externalStoreId() != null && storeLocationId == null) {
                    review++;
                    continue;
                }

                Product product = productRepository.findBySkuIgnoreCase(normalized.catalogKey())
                        .orElseGet(() -> productRepository.save(new Product(
                                normalized.brand(), normalized.modelName(), normalized.catalogKey(),
                                normalized.categoryCode(), null)));
                link(source, SourceEntityType.PRODUCT_MODEL, "model:" + normalized.catalogKey(),
                        product.getId(), item.sourceUri().toString(), normalized.catalogKey(), scan.fetchedAt());

                boolean[] variantCreated = {false};
                ProductVariant variant = variantRepository
                        .findByProductModelIdAndCanonicalKey(product.getId(), normalized.variantKey())
                        .orElseGet(() -> {
                            variantCreated[0] = true;
                            return variantRepository.save(new ProductVariant(
                                    product, normalized.variantKey(), null, normalized.variantDisplayName()));
                        });
                if (variantCreated[0]) {
                    saveAttributes(variant, normalized, item);
                }
                link(source, SourceEntityType.PRODUCT_VARIANT, item.externalId(), variant.getId(),
                        item.sourceUri().toString(), normalized.variantKey(), scan.fetchedAt());

                OfferSnapshot snapshot = new OfferSnapshot(
                        source.getMarket().getId(),
                        storeLocationId,
                        variant.getId(),
                        item.externalId(),
                        source.getConnectorKey() + ":" + item.externalId(),
                        item.conditionType(),
                        item.regularPrice(),
                        item.salePrice(),
                        item.conditionalPrice(),
                        item.currency(),
                        item.availabilityStatus(),
                        item.quantity(),
                        item.sourceUri().toString(),
                        scan.fetchedAt(),
                        scan.fetchedAt().plus(Duration.ofDays(2)),
                        item.terms());
                OfferUpsertResult result = offerIngestionService.ingest(snapshot, job.getCollectionRun());
                if (result.created()) {
                    created++;
                }
                if (result.stateChanged()) {
                    changed++;
                }
                link(source, SourceEntityType.OFFER, item.externalId(), result.offerId(),
                        item.sourceUri().toString(), normalized.variantKey(), scan.fetchedAt());
            } catch (RuntimeException exception) {
                upsertReview(source, job, item, ReviewType.PRODUCT_MATCH,
                        concise(exception), scan.fetchedAt());
                review++;
                errors++;
            }
        }

        return new ParserJobOutcome(scan.items().size(), created, changed, review, errors);
    }

    /** Сопоставляет внешний филиал по устойчивой связи или ID сети; неизвестный филиал отправляет на проверку. */
    private UUID resolveStore(ParserSource source, ParserJob job, ParsedCatalogItem item, java.time.Instant observedAt) {
        if (item.externalStoreId() == null || item.externalStoreId().isBlank()) {
            return null;
        }
        Optional<SourceEntityLink> linked = linkRepository
                .findByParserSourceIdAndEntityTypeAndExternalId(
                        source.getId(), SourceEntityType.STORE_LOCATION, item.externalStoreId());
        if (linked.isPresent()) {
            return linked.get().getInternalEntityId();
        }

        UUID chainId = source.getMarket().getRetailChain().getId();
        Optional<Store> direct = storeRepository.findByRetailChainIdAndExternalStoreId(chainId, item.externalStoreId());
        if (direct.isPresent()) {
            Store store = direct.get();
            link(source, SourceEntityType.STORE_LOCATION, item.externalStoreId(), store.getId(),
                    item.sourceUri().toString(), store.getExternalStoreId(), observedAt);
            return store.getId();
        }

        upsertReview(source, job, item, ReviewType.STORE_MATCH,
                "Цена относится к филиалу, который ещё не сопоставлен", observedAt);
        return null;
    }

    /** Создаёт значения фильтруемых характеристик только для новой конфигурации товара. */
    private void saveAttributes(ProductVariant variant, NormalizedProductCandidate normalized,
                                ParsedCatalogItem rawItem) {
        normalized.attributes().forEach((code, normalizedValue) -> {
            AttributeDefinition definition = attributeRepository.findByCode(code)
                    .orElseGet(() -> attributeRepository.save(definition(code)));
            String displayValue = rawItem.attributes().getOrDefault(code, normalizedValue);
            variantAttributeRepository.save(new ProductVariantAttribute(
                    variant,
                    definition,
                    normalizedValue,
                    displayValue,
                    numericValue(code, normalizedValue),
                    null));
        });
    }

    /** Возвращает описание известной характеристики для каталога и фильтров. */
    private static AttributeDefinition definition(String code) {
        return switch (code) {
            case "storage" -> new AttributeDefinition(code, "Объём памяти", AttributeDataType.NUMBER, "GB", true);
            case "color" -> new AttributeDefinition(code, "Цвет", AttributeDataType.TEXT, null, true);
            case "sim" -> new AttributeDefinition(code, "Тип SIM-карты", AttributeDataType.TEXT, null, true);
            default -> new AttributeDefinition(code, code, AttributeDataType.TEXT, null, true);
        };
    }

    /** Переводит числовую характеристику в единую единицу; для памяти это гигабайты. */
    private static BigDecimal numericValue(String code, String value) {
        if (!code.equals("storage")) {
            return null;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        BigDecimal amount = new BigDecimal(lower.replaceAll("[^0-9.]", ""));
        return lower.contains("tb") || lower.contains("тб") ? amount.multiply(BigDecimal.valueOf(1024)) : amount;
    }

    /** Создаёт либо обновляет одну карточку ручной проверки по стабильному отпечатку. */
    private void upsertReview(ParserSource source, ParserJob job, ParsedCatalogItem item,
                              ReviewType type, String reason, java.time.Instant observedAt) {
        String fingerprint = fingerprintService.sha256(
                type + "|" + item.rawName().toLowerCase(Locale.ROOT) + "|" + item.sourceUri());
        Map<String, Object> payload = new LinkedHashMap<>(item.rawPayload());
        payload.put("attributes", item.attributes());
        payload.put("regularPrice", item.regularPrice());
        payload.put("salePrice", item.salePrice());
        reviewRepository.findByParserSourceIdAndReviewTypeAndFingerprint(source.getId(), type, fingerprint)
                .ifPresentOrElse(
                        existing -> existing.observeAgain(job, item.sourceUri().toString(), payload, reason, observedAt),
                        () -> reviewRepository.save(new ReviewCandidate(
                                source, job, type, fingerprint, item.rawName(), item.sourceCategory(),
                                item.sourceUri().toString(), item.externalId(), null, null,
                                reason, payload, observedAt)));
    }

    /** Создаёт либо освежает связь внешнего ID с канонической сущностью. */
    private void link(ParserSource source, SourceEntityType entityType, String externalId,
                      UUID internalId, String sourceUrl, String identity, java.time.Instant observedAt) {
        String fingerprint = fingerprintService.sha256(entityType + "|" + identity.toLowerCase(Locale.ROOT));
        linkRepository.findByParserSourceIdAndEntityTypeAndExternalId(source.getId(), entityType, externalId)
                .ifPresentOrElse(
                        existing -> existing.observeAgain(sourceUrl, fingerprint, observedAt),
                        () -> linkRepository.save(new SourceEntityLink(
                                source, entityType, externalId, internalId, sourceUrl, fingerprint, observedAt)));
    }

    /** Подготавливает короткое диагностическое сообщение для карточки проверки. */
    private static String concise(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() <= 900 ? message : message.substring(0, 900);
    }
}
