package com.pricehunter.offer;

import com.pricehunter.history.OfferStateHistoryRepository;
import com.pricehunter.product.ProductVariantRepository;
import com.pricehunter.shared.NotFoundException;
import com.pricehunter.shared.SliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read-only фасад предложений, графиков, остатков и межгородской доставки.
 * Переиспользует проекции репозиториев, чтобы публичные контроллеры не загружали граф JPA-сущностей.
 */
@Service
@RequiredArgsConstructor
public class OfferQueryService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Duration DEFAULT_HISTORY_PERIOD = Duration.ofDays(180);

    private final OfferRepository offerRepository;
    private final OfferTermRepository offerTermRepository;
    private final OfferStateHistoryRepository historyRepository;
    private final StoreInventoryRepository inventoryRepository;
    private final ShippingQuoteRepository shippingQuoteRepository;
    private final ProductVariantRepository variantRepository;

    /** Возвращает предложения варианта в городе, отсортированные по актуальной цене по возрастанию. */
    @Transactional(readOnly = true)
    public SliceResponse<OfferResponse> findVariantOffers(UUID variantId, UUID cityId, int page, int size) {
        require(cityId, "cityId");
        return SliceResponse.from(offerRepository.findBestLocalOffers(variantId, cityId, page(page, size)),
                projection -> OfferResponse.from(projection, List.of()));
    }

    /** Возвращает одну полную карточку предложения, включая актуальные коммерческие условия. */
    @Transactional(readOnly = true)
    public OfferResponse findOffer(UUID offerId) {
        var projection = offerRepository.findCard(offerId)
                .orElseThrow(() -> new NotFoundException("Offer '" + offerId + "' was not found"));
        List<OfferTermResponse> terms = offerTermRepository.findActiveTerms(offerId, Instant.now()).stream()
                .map(OfferTermResponse::from)
                .toList();
        return OfferResponse.from(projection, terms);
    }

    /** Возвращает интервалы истории для непосредственного построения графика. */
    @Transactional(readOnly = true)
    public List<PriceHistoryResponse> findHistory(UUID offerId, Instant from, Instant to) {
        ensureOfferExists(offerId);
        Instant end = to == null ? Instant.now() : to;
        Instant start = from == null ? end.minus(DEFAULT_HISTORY_PERIOD) : from;
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("from must be earlier than to");
        }
        return historyRepository.findChartRange(offerId, start, end).stream().map(PriceHistoryResponse::from).toList();
    }

    /** Возвращает остатки предложения по филиалам детерминированными порциями. */
    @Transactional(readOnly = true)
    public SliceResponse<InventoryResponse> findInventory(UUID offerId, int page, int size) {
        ensureOfferExists(offerId);
        return SliceResponse.from(inventoryRepository.findOfferInventory(offerId, page(page, size)),
                InventoryResponse::from);
    }

    /** Возвращает отсортированные по общей цене варианты доставки в целевой город. */
    @Transactional(readOnly = true)
    public SliceResponse<DeliveredOfferResponse> findDeliveredOffers(UUID variantId, UUID cityId, int page, int size) {
        require(cityId, "cityId");
        if (!variantRepository.existsById(variantId)) {
            throw new NotFoundException("Variant '" + variantId + "' was not found");
        }
        return SliceResponse.from(shippingQuoteRepository.findBestDeliveredOffers(variantId, cityId, page(page, size)),
                DeliveredOfferResponse::from);
    }

    /** Подтверждает существование активного предложения перед чтением связанных данных. */
    private void ensureOfferExists(UUID offerId) {
        if (offerRepository.findCard(offerId).isEmpty()) {
            throw new NotFoundException("Offer '" + offerId + "' was not found");
        }
    }

    /** Ограничивает размер потенциально больших выдач. */
    private static Pageable page(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to zero");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be greater than zero");
        }
        return PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
    }

    /** Проверяет обязательный идентификатор до обращения к репозиторию. */
    private static void require(UUID value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
