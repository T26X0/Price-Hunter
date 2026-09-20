package com.pricehunter.offer;

import com.pricehunter.history.OfferStateHistory;
import com.pricehunter.history.OfferStateHistoryRepository;
import com.pricehunter.product.ProductVariant;
import com.pricehunter.product.ProductVariantRepository;
import com.pricehunter.retail.ChainCityMarket;
import com.pricehunter.retail.ChainCityMarketRepository;
import com.pricehunter.store.Store;
import com.pricehunter.store.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
/** Идемпотентно обновляет предложение под блокировкой и ведёт интервальную историю. */
public class OfferIngestionService {

    private final OfferRepository offerRepository;
    private final OfferStateHistoryRepository historyRepository;
    private final ChainCityMarketRepository marketRepository;
    private final ProductVariantRepository variantRepository;
    private final StoreRepository storeRepository;
    private final OfferStateHasher stateHasher;
    private final OfferTermRepository offerTermRepository;

    /** Создаёт предложение либо обновляет его; новую историю пишет только при изменении состояния. */
    @Transactional
    public OfferUpsertResult ingest(OfferSnapshot snapshot, CollectionRun sourceRun) {
        String stateHash = stateHasher.hash(snapshot);
        Optional<Offer> existing = snapshot.externalOfferId() == null || snapshot.externalOfferId().isBlank()
                ? offerRepository.findForUpdateByOfferKey(
                        snapshot.marketId(), snapshot.storeLocationId(), snapshot.offerKey())
                : offerRepository.findForUpdateByExternalId(
                        snapshot.marketId(), snapshot.storeLocationId(), snapshot.externalOfferId());

        if (existing.isEmpty()) {
            ChainCityMarket market = marketRepository.getReferenceById(snapshot.marketId());
            ProductVariant variant = variantRepository.getReferenceById(snapshot.productVariantId());
            Store store = snapshot.storeLocationId() == null
                    ? null
                    : storeRepository.getReferenceById(snapshot.storeLocationId());
            Offer created = new Offer(
                    market,
                    store,
                    variant,
                    snapshot.externalOfferId(),
                    snapshot.offerKey(),
                    snapshot.conditionType(),
                    snapshot.regularPrice(),
                    snapshot.salePrice(),
                    snapshot.conditionalPrice(),
                    snapshot.currency(),
                    snapshot.availabilityStatus(),
                    snapshot.quantity(),
                    snapshot.productUrl(),
                    snapshot.observedAt(),
                    stateHash
            );
            created.refresh(snapshot.regularPrice(), snapshot.salePrice(), snapshot.conditionalPrice(),
                    snapshot.availabilityStatus(), snapshot.quantity(), snapshot.productUrl(),
                    snapshot.observedAt(), snapshot.freshUntil(), stateHash);
            offerRepository.save(created);
            synchronizeCurrentTerms(created, snapshot.terms());
            historyRepository.save(new OfferStateHistory(created, sourceRun, snapshot.terms(), snapshot.observedAt()));
            return new OfferUpsertResult(created.getId(), true, true);
        }

        Offer offer = existing.get();
        boolean changed = !offer.getStateHash().equals(stateHash);
        if (changed) {
            historyRepository.findOpenStateForUpdate(offer.getId())
                    .ifPresent(history -> history.closeAt(snapshot.observedAt()));
            historyRepository.flush();
        }

        offer.refresh(snapshot.regularPrice(), snapshot.salePrice(), snapshot.conditionalPrice(),
                snapshot.availabilityStatus(), snapshot.quantity(), snapshot.productUrl(),
                snapshot.observedAt(), snapshot.freshUntil(), stateHash);

        if (changed) {
            synchronizeCurrentTerms(offer, snapshot.terms());
            historyRepository.save(new OfferStateHistory(offer, sourceRun, snapshot.terms(), snapshot.observedAt()));
        }
        return new OfferUpsertResult(offer.getId(), false, changed);
    }

    /**
     * Заменяет текущие условия только при смене состояния offer.
     * Исторический снимок остаётся в offer_state_history, а эта таблица хранит удобное для карточки актуальное состояние.
     */
    private void synchronizeCurrentTerms(Offer offer, List<Map<String, Object>> rawTerms) {
        offerTermRepository.findByOfferIdAndActiveTrue(offer.getId()).forEach(OfferTerm::deactivate);
        rawTerms.stream().map(raw -> termFrom(offer, raw)).forEach(offerTermRepository::save);
    }

    /** Преобразует нормализованное описание условия коннектора в текущую сущность условия покупки. */
    private static OfferTerm termFrom(Offer offer, Map<String, Object> raw) {
        OfferTermType type = termType(raw.get("type"));
        String title = text(raw.get("title"));
        if (title == null) {
            title = type.name();
        }
        return new OfferTerm(offer, type, title, text(raw.get("description")), amount(raw.get("monetaryValue")),
                amount(raw.get("annualRate")), shortValue(raw.get("termMonths")), amount(raw.get("monthlyPayment")),
                amount(raw.get("initialPayment")), null, null, text(raw.get("termsUrl")), raw);
    }

    /** Сопоставляет ключи адаптеров с внутренним перечислением без падения из-за неизвестной промо-механики. */
    private static OfferTermType termType(Object value) {
        if (value == null) {
            return OfferTermType.OTHER;
        }
        String raw = value.toString().trim().toUpperCase();
        if (raw.equals("DISCOUNT_CONDITION")) {
            return OfferTermType.DISCOUNT;
        }
        try {
            return OfferTermType.valueOf(raw);
        } catch (IllegalArgumentException exception) {
            return OfferTermType.OTHER;
        }
    }

    /** Возвращает непустой текст либо null для необязательного поля. */
    private static String text(Object value) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return value.toString().trim();
    }

    /** Безопасно преобразует число или строку с числом в денежное значение. */
    private static BigDecimal amount(Object value) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.toString().trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /** Безопасно преобразует короткое целое число из снимка. */
    private static Short shortValue(Object value) {
        BigDecimal amount = amount(value);
        return amount == null ? null : amount.shortValueExact();
    }
}
