package com.pricehunter.offer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Component
public class OfferStateHasher {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    public String hash(OfferSnapshot snapshot) {
        List<String> normalizedTerms = new ArrayList<>();
        for (Map<String, Object> term : snapshot.terms()) {
            try {
                normalizedTerms.add(objectMapper.writeValueAsString(new TreeMap<>(term)));
            } catch (JsonProcessingException exception) {
                throw new IllegalArgumentException("Offer terms cannot be serialized", exception);
            }
        }
        normalizedTerms.sort(String::compareTo);

        String source = String.join("|",
                amount(snapshot.regularPrice()),
                amount(snapshot.salePrice()),
                amount(snapshot.conditionalPrice()),
                snapshot.currency().trim().toUpperCase(),
                snapshot.availabilityStatus().name(),
                snapshot.quantity() == null ? "" : snapshot.quantity().toString(),
                String.join(";", normalizedTerms));

        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static String amount(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }
}
