package com.pricehunter.parser.http;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/** Проверяет, что парсер не может использовать production-сеть для обращения к внутренним адресам. */
class JavaParserHttpClientSecurityTest {

    /** Отклоняет loopback, private, link-local, CGNAT, multicast и IPv6 ULA диапазоны. */
    @Test
    void rejectsNonPublicAddressesBeforeRequestingRobotsOrPage() {
        assertRejected("https://127.0.0.1/catalog");
        assertRejected("https://10.0.0.1/catalog");
        assertRejected("https://100.64.0.1/catalog");
        assertRejected("https://169.254.169.254/latest/meta-data");
        assertRejected("https://172.16.0.1/catalog");
        assertRejected("https://192.168.1.1/catalog");
        assertRejected("https://224.0.0.1/catalog");
        assertRejected("https://[fc00::1]/catalog");
        assertRejected("https://[::1]/catalog");
    }

    /** Делает короткую проверку каждого запрещённого URL без реального HTTP-запроса. */
    private static void assertRejected(String rawUrl) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> JavaParserHttpClient.validatePublicHttpsUri(URI.create(rawUrl)));
    }
}
