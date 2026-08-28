package com.pricehunter.parser.http;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
/**
 * Безопасная сетевая реализация для парсеров: только публичный HTTPS, robots.txt,
 * задержка между запросами, запрет редиректов и ограничение размера ответа.
 */
public class JavaParserHttpClient implements ParserHttpClient {

    private static final int MAX_BODY_BYTES = 12 * 1024 * 1024;
    private static final Duration ROBOTS_TTL = Duration.ofHours(12);
    private static final String USER_AGENT = "PriceHunter/0.1 catalog-monitor";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private final Map<String, CachedRobots> robotsCache = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastRequests = new ConcurrentHashMap<>();

    /** Проверяет адрес и правила сайта, соблюдает задержку и возвращает страницу. */
    @Override
    public String get(URI uri) {
        validatePublicHttpsUri(uri);
        RobotsRules robots = robotsFor(uri);
        if (!robots.allows(uri)) {
            throw new ParserHttpException("robots.txt does not allow parsing " + uri.getPath());
        }
        waitForHost(uri.getHost(), robots.crawlDelay());
        return request(uri);
    }

    /** Возвращает правила robots.txt из кэша либо обновляет их не чаще одного раза в 12 часов. */
    private RobotsRules robotsFor(URI uri) {
        String origin = origin(uri);
        CachedRobots cached = robotsCache.get(origin);
        Instant now = Instant.now();
        if (cached != null && cached.fetchedAt().plus(ROBOTS_TTL).isAfter(now)) {
            return cached.rules();
        }

        URI robotsUri = URI.create(origin + "/robots.txt");
        RobotsRules rules;
        try {
            String content = request(robotsUri);
            rules = RobotsRules.parse(content);
        } catch (ParserHttpException exception) {
            rules = RobotsRules.allowAll();
        }
        robotsCache.put(origin, new CachedRobots(rules, now));
        return rules;
    }

    /** Выполняет один HTTP-запрос и проверяет статус и максимальный размер тела. */
    private String request(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,text/plain;q=0.8")
                .GET()
                .build();
        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ParserHttpException("Source returned HTTP " + response.statusCode());
            }
            try (InputStream body = response.body()) {
                byte[] bytes = body.readNBytes(MAX_BODY_BYTES + 1);
                if (bytes.length > MAX_BODY_BYTES) {
                    throw new ParserHttpException("Source response exceeds 12 MiB limit");
                }
                return new String(bytes, StandardCharsets.UTF_8);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ParserHttpException("Parser request was interrupted", exception);
        } catch (IOException exception) {
            throw new ParserHttpException("Cannot read parser source", exception);
        }
    }

    /** При необходимости приостанавливает текущий поток до разрешённого момента запроса к домену. */
    private void waitForHost(String host, Duration delay) {
        synchronized (lastRequests) {
            Instant now = Instant.now();
            Instant earliest = lastRequests.getOrDefault(host, Instant.EPOCH).plus(delay);
            if (earliest.isAfter(now)) {
                try {
                    Thread.sleep(Duration.between(now, earliest).toMillis());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new ParserHttpException("Parser rate-limit wait was interrupted", exception);
                }
            }
            lastRequests.put(host, Instant.now());
        }
    }

    /** Блокирует локальные адреса и небезопасные формы URI до любого сетевого обращения. */
    private static void validatePublicHttpsUri(URI uri) {
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                || uri.getFragment() != null) {
            throw new IllegalArgumentException("Parser sources must use a public HTTPS URL");
        }
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (host.equals("localhost") || host.endsWith(".local") || host.equals("127.0.0.1")
                || host.equals("0.0.0.0") || host.equals("::1")) {
            throw new IllegalArgumentException("Local addresses cannot be parser sources");
        }
    }

    /** Строит канонический origin, используемый как ключ кэша robots.txt. */
    private static String origin(URI uri) {
        int port = uri.getPort();
        return uri.getScheme().toLowerCase(Locale.ROOT) + "://" + uri.getHost().toLowerCase(Locale.ROOT)
                + (port == -1 ? "" : ":" + port);
    }

    /** Правила robots.txt вместе со временем их загрузки. */
    private record CachedRobots(RobotsRules rules, Instant fetchedAt) {
    }
}
