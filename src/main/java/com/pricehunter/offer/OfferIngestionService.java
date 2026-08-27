package com.pricehunter.offer;

import com.pricehunter.history.OfferStateHistory;
import com.pricehunter.history.OfferStateHistoryRepository;
import com.pricehunter.product.ProductVariant;
import com.pricehunter.product.ProductVariantRepository;
import com.pricehunter.retail.ChainCityMarket;
import com.pricehunter.retail.ChainCityMarketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OfferIngestionService {

    private final OfferRepository offerRepository;
    private final OfferStateHistoryRepository historyRepository;
    private final ChainCityMarketRepository marketRepository;
    private final ProductVariantRepository variantRepository;
    private final OfferStateHasher stateHasher;

    @Transactional
    public OfferUpsertResult ingest(OfferSnapshot snapshot, CollectionRun sourceRun) {
        String stateHash = stateHasher.hash(snapshot);
        Optional<Offer> existing = snapshot.externalOfferId() == null || snapshot.externalOfferId().isBlank()
                ? offerRepository.findForUpdateByOfferKey(snapshot.marketId(), snapshot.offerKey())
                : offerRepository.findForUpdateByExternalId(snapshot.marketId(), snapshot.externalOfferId());

        if (existing.isEmpty()) {
            ChainCityMarket market = marketRepository.getReferenceById(snapshot.marketId());
            ProductVariant variant = variantRepository.getReferenceById(snapshot.productVariantId());
            Offer created = new Offer(
                    market,
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
            historyRepository.save(new OfferStateHistory(offer, sourceRun, snapshot.terms(), snapshot.observedAt()));
        }
        return new OfferUpsertResult(offer.getId(), false, changed);
    }
}
