package com.pricehunter.offer;

import com.pricehunter.city.City;
import com.pricehunter.city.CityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ShippingQuoteIngestionService {

    private final ShippingQuoteRepository shippingQuoteRepository;
    private final OfferRepository offerRepository;
    private final CityRepository cityRepository;

    @Transactional
    public ShippingQuoteUpsertResult ingest(ShippingQuoteSnapshot snapshot) {
        String stateHash = stateHash(snapshot);
        Optional<ShippingQuote> existing = shippingQuoteRepository.findForUpdate(
                snapshot.offerId(), snapshot.destinationCityId());

        if (existing.isPresent()) {
            ShippingQuote quote = existing.get();
            boolean changed = !quote.getStateHash().equals(stateHash);
            quote.refresh(snapshot.available(), snapshot.deliveryPrice(), snapshot.currency(),
                    snapshot.minDeliveryDays(), snapshot.maxDeliveryDays(), snapshot.observedAt(), stateHash);
            return new ShippingQuoteUpsertResult(quote.getId(), false, changed);
        }

        Offer offer = offerRepository.getReferenceById(snapshot.offerId());
        City destinationCity = cityRepository.getReferenceById(snapshot.destinationCityId());
        ShippingQuote created = shippingQuoteRepository.save(new ShippingQuote(
                offer, destinationCity, snapshot.available(), snapshot.deliveryPrice(), snapshot.currency(),
                snapshot.minDeliveryDays(), snapshot.maxDeliveryDays(), snapshot.observedAt(), stateHash));
        return new ShippingQuoteUpsertResult(created.getId(), true, true);
    }

    private static String stateHash(ShippingQuoteSnapshot snapshot) {
        String source = String.join("|",
                Boolean.toString(snapshot.available()),
                amount(snapshot.deliveryPrice()),
                snapshot.currency().trim().toUpperCase(),
                snapshot.minDeliveryDays() == null ? "" : snapshot.minDeliveryDays().toString(),
                snapshot.maxDeliveryDays() == null ? "" : snapshot.maxDeliveryDays().toString());
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static String amount(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }
}
