package com.pricehunter.parser.identity;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
/** Создаёт воспроизводимые SHA-256 отпечатки для дедупликации внешних данных. */
public class FingerprintService {

    /** @return шестнадцатеричный SHA-256 переданной строки в UTF-8 */
    public String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
