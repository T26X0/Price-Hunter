package com.pricehunter.offer;

import com.pricehunter.history.StoreInventoryHistory;
import com.pricehunter.history.StoreInventoryHistoryRepository;
import com.pricehunter.store.Store;
import com.pricehunter.store.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class StoreInventoryIngestionService {

    private final StoreInventoryRepository inventoryRepository;
    private final StoreInventoryHistoryRepository historyRepository;
    private final OfferRepository offerRepository;
    private final StoreRepository storeRepository;

    @Transactional
    public boolean ingest(InventorySnapshot snapshot, CollectionRun sourceRun) {
        String stateHash = hash(snapshot);
        var existing = inventoryRepository.findForUpdate(snapshot.offerId(), snapshot.storeLocationId());
        if (existing.isEmpty()) {
            Offer offer = offerRepository.getReferenceById(snapshot.offerId());
            Store store = storeRepository.getReferenceById(snapshot.storeLocationId());
            StoreInventory inventory = new StoreInventory(offer, store, snapshot.availabilityStatus(),
                    snapshot.quantity(), snapshot.observedAt(), stateHash);
            inventoryRepository.save(inventory);
            historyRepository.save(new StoreInventoryHistory(offer, store, sourceRun,
                    snapshot.availabilityStatus(), snapshot.quantity(), stateHash, snapshot.observedAt()));
            return true;
        }

        StoreInventory inventory = existing.get();
        if (inventory.getStateHash().equals(stateHash)) {
            inventory.refresh(snapshot.availabilityStatus(), snapshot.quantity(), snapshot.observedAt(), stateHash);
            return false;
        }

        historyRepository.findOpenStateForUpdate(snapshot.offerId(), snapshot.storeLocationId())
                .ifPresent(history -> history.closeAt(snapshot.observedAt()));
        historyRepository.flush();
        inventory.refresh(snapshot.availabilityStatus(), snapshot.quantity(), snapshot.observedAt(), stateHash);
        historyRepository.save(new StoreInventoryHistory(inventory.getOffer(), inventory.getStoreLocation(),
                sourceRun, snapshot.availabilityStatus(), snapshot.quantity(), stateHash, snapshot.observedAt()));
        return true;
    }

    private static String hash(InventorySnapshot snapshot) {
        String source = snapshot.availabilityStatus().name() + "|"
                + (snapshot.quantity() == null ? "" : snapshot.quantity());
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
