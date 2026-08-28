package com.pricehunter.offer;

import com.pricehunter.history.OfferStateHistory;
import com.pricehunter.history.OfferStateHistoryRepository;
import com.pricehunter.product.ProductVariantRepository;
import com.pricehunter.retail.ChainCityMarketRepository;
import com.pricehunter.store.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfferIngestionServiceTest {

    @Mock OfferRepository offerRepository;
    @Mock OfferStateHistoryRepository historyRepository;
    @Mock ChainCityMarketRepository marketRepository;
    @Mock ProductVariantRepository variantRepository;
    @Mock StoreRepository storeRepository;
    @Mock OfferStateHasher stateHasher;
    @Mock Offer offer;
    @Mock OfferStateHistory openHistory;

    private OfferIngestionService service;

    @BeforeEach
    void setUp() {
        service = new OfferIngestionService(offerRepository, historyRepository,
                marketRepository, variantRepository, storeRepository, stateHasher);
    }

    @Test
    void doesNotAppendHistoryWhenStateIsUnchanged() {
        OfferSnapshot snapshot = snapshot();
        UUID offerId = UUID.randomUUID();
        when(stateHasher.hash(snapshot)).thenReturn("same-hash");
        when(offerRepository.findForUpdateByExternalId(
                snapshot.marketId(), snapshot.storeLocationId(), snapshot.externalOfferId()))
                .thenReturn(Optional.of(offer));
        when(offer.getId()).thenReturn(offerId);
        when(offer.getStateHash()).thenReturn("same-hash");

        OfferUpsertResult result = service.ingest(snapshot, null);

        assertThat(result).isEqualTo(new OfferUpsertResult(offerId, false, false));
        verify(historyRepository, never()).save(any());
        verify(historyRepository, never()).findOpenStateForUpdate(any());
        verify(offer).refresh(snapshot.regularPrice(), snapshot.salePrice(), snapshot.conditionalPrice(),
                snapshot.availabilityStatus(), snapshot.quantity(), snapshot.productUrl(),
                snapshot.observedAt(), snapshot.freshUntil(), "same-hash");
    }

    @Test
    void closesOldIntervalAndAppendsChangedState() {
        OfferSnapshot snapshot = snapshot();
        UUID offerId = UUID.randomUUID();
        when(stateHasher.hash(snapshot)).thenReturn("new-hash");
        when(offerRepository.findForUpdateByExternalId(
                snapshot.marketId(), snapshot.storeLocationId(), snapshot.externalOfferId()))
                .thenReturn(Optional.of(offer));
        when(offer.getId()).thenReturn(offerId);
        when(offer.getStateHash()).thenReturn("old-hash");
        when(historyRepository.findOpenStateForUpdate(offerId)).thenReturn(Optional.of(openHistory));

        OfferUpsertResult result = service.ingest(snapshot, null);

        assertThat(result).isEqualTo(new OfferUpsertResult(offerId, false, true));
        verify(openHistory).closeAt(snapshot.observedAt());
        verify(historyRepository).save(any(OfferStateHistory.class));
    }

    private static OfferSnapshot snapshot() {
        return new OfferSnapshot(
                UUID.randomUUID(), null, UUID.randomUUID(), "external-1", "variant:new",
                ConditionType.NEW, new BigDecimal("115000"), null, null, "RUB",
                AvailabilityStatus.IN_STOCK, 5, "https://example.test/product",
                Instant.parse("2026-08-27T00:00:00Z"),
                Instant.parse("2026-08-29T00:00:00Z"), List.of());
    }
}
