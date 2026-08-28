package com.pricehunter.parser.connector.ninefortyone;

import com.pricehunter.offer.AvailabilityStatus;
import com.pricehunter.offer.ConditionType;
import com.pricehunter.parser.ConnectorMode;
import com.pricehunter.parser.ParserSourceType;
import com.pricehunter.parser.connector.CatalogConnector;
import com.pricehunter.parser.connector.CatalogScanRequest;
import com.pricehunter.parser.connector.CatalogScanResult;
import com.pricehunter.parser.connector.ParsedCatalogItem;
import com.pricehunter.parser.http.ParserHttpClient;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
/**
 * HTML-адаптер каталога сети 941.
 * Читает стабильную Schema.org-разметку и преобразует карточки в общий формат парсерного конвейера.
 */
public class NineFortyOneCatalogConnector implements CatalogConnector {

    private static final Pattern MONEY = Pattern.compile("^(\\d[\\d\\s\\u00a0]*)\\s*₽$");
    private static final Pattern STORAGE = Pattern.compile("(?i)\\b(128|256|512)\\s*G(?:B|b)\\b|\\b1\\s*T(?:B|b)\\b");
    private static final Pattern PRODUCT_PATH = Pattern.compile("^/product/([^/?#]+)$");
    private static final String PRODUCT_LINK_SELECTOR =
            "a[itemscope][itemtype=\"https://schema.org/Product\"][href^=/product/]";
    private static final String PRODUCT_CONTAINER_SELECTOR =
            "[itemprop=itemListElement]:has(" + PRODUCT_LINK_SELECTOR + ")"
                    + ":has([itemprop=name]):has([itemprop=offers])";

    private final ParserHttpClient httpClient;

    /** @return тип источника «сайт магазина» */
    @Override
    public ParserSourceType sourceType() {
        return ParserSourceType.WEBSITE;
    }

    /** @return режим прямого чтения HTML без браузера */
    @Override
    public ConnectorMode mode() {
        return ConnectorMode.HTML;
    }

    /** @return стабильный ключ адаптера сети 941 */
    @Override
    public String connectorKey() {
        return "941";
    }

    /** Загружает настроенную страницу 941 безопасным HTTP-клиентом и разбирает ответ. */
    @Override
    public CatalogScanResult scan(CatalogScanRequest request) {
        validateHost(request.catalogUri());
        String html = httpClient.get(request.catalogUri());
        return parse(html, request.catalogUri(), request.observedAt());
    }

    /**
     * Разбирает уже полученный HTML; отдельный метод позволяет тестировать адаптер без сети.
     * Повторяющиеся внешние ID схлопываются, а отсутствие полноценных карточек считается ошибкой.
     */
    public CatalogScanResult parse(String html, URI sourceUri, Instant fetchedAt) {
        Document document = Jsoup.parse(html, sourceUri.toString());
        Map<String, ParsedCatalogItem> unique = new LinkedHashMap<>();

        List<Element> cards = new ArrayList<>(document.select(PRODUCT_CONTAINER_SELECTOR));
        if (cards.isEmpty()) {
            cards.addAll(document.select(PRODUCT_LINK_SELECTOR
                    + ":has([itemprop=name]):has([itemprop=offers])"));
        }
        for (Element card : cards) {
            ParsedCatalogItem item = parseCard(card, sourceUri);
            unique.putIfAbsent(item.externalId(), item);
        }
        if (unique.isEmpty()) {
            throw new IllegalArgumentException("941 catalog page contains no structured product cards");
        }
        return new CatalogScanResult(sourceUri, fetchedAt, List.copyOf(unique.values()));
    }

    /** Преобразует одну карточку 941 в унифицированную позицию каталога. */
    private ParsedCatalogItem parseCard(Element card, URI pageUri) {
        Element productLink = card.is(PRODUCT_LINK_SELECTOR)
                ? card : required(card, PRODUCT_LINK_SELECTOR);
        URI productUri = pageUri.resolve(productLink.attr("href"));
        String externalId = externalId(productUri);
        String name = requiredAttributeOrText(card, "[itemprop=name]", "content");
        Element offer = required(card, "[itemprop=offers]");
        BigDecimal currentPrice = amount(required(offer, "[itemprop=price]").attr("content"));
        String currency = required(offer, "[itemprop=priceCurrency]").attr("content").trim().toUpperCase(Locale.ROOT);
        AvailabilityStatus availability = availability(offer.selectFirst("[itemprop=availability]"));
        ConditionType condition = condition(offer.selectFirst("[itemprop=itemCondition]"));
        BigDecimal regularPrice = regularPrice(card, currentPrice);
        BigDecimal salePrice = regularPrice == null ? null : currentPrice;
        if (regularPrice == null) {
            regularPrice = currentPrice;
        }

        Map<String, String> attributes = attributes(name, card.text());
        List<Map<String, Object>> terms = terms(card);
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("schema", "https://schema.org/Product");
        raw.put("page", pageUri.toString());
        raw.put("productUrl", productUri.toString());
        raw.put("rawName", name);

        return new ParsedCatalogItem(
                externalId,
                null,
                productUri,
                name,
                brand(name),
                "smartphones",
                condition,
                regularPrice,
                salePrice,
                null,
                currency,
                availability,
                null,
                attributes,
                terms,
                raw
        );
    }

    /** Извлекает фильтруемые память, SIM и цвет из названия и текста карточки. */
    private static Map<String, String> attributes(String name, String cardText) {
        Map<String, String> attributes = new LinkedHashMap<>();
        Matcher storage = STORAGE.matcher(name);
        if (storage.find()) {
            String raw = storage.group().replaceAll("\\s+", " ").trim();
            attributes.put("storage", raw.equalsIgnoreCase("1Tb") || raw.equalsIgnoreCase("1 TB")
                    ? "1 TB" : raw.toUpperCase(Locale.ROOT).replace("GB", " GB"));
        }

        String lower = cardText.toLowerCase(Locale.ROOT);
        if (lower.contains("nano sim+esim")) {
            attributes.put("sim", "nano SIM+eSIM");
        } else if (lower.contains("dual sim")) {
            attributes.put("sim", "Dual SIM");
        } else if (lower.contains("esim")) {
            attributes.put("sim", "eSIM");
        }

        int comma = name.lastIndexOf(',');
        if (comma >= 0 && comma + 1 < name.length()) {
            attributes.put("color", name.substring(comma + 1).trim());
        }
        return attributes;
    }

    /** Преобразует надписи о рассрочке, подарках и выгоде в структурированные условия. */
    private static List<Map<String, Object>> terms(Element card) {
        List<Map<String, Object>> terms = new ArrayList<>();
        String text = card.text().toLowerCase(Locale.ROOT);
        if (text.contains("рассрочка")) {
            terms.add(Map.of("type", "INSTALLMENT", "title", "Рассрочка"));
        }
        if (text.contains("подарки")) {
            terms.add(Map.of("type", "GIFT", "title", "Подарки"));
        }
        Matcher benefit = Pattern.compile("(?iu)выгода[^0-9]*(\\d[\\d\\s\\u00a0]*)\\s*₽")
                .matcher(card.text());
        if (benefit.find()) {
            terms.add(Map.of(
                    "type", "DISCOUNT_CONDITION",
                    "title", "Выгода " + benefit.group(1).trim() + " ₽",
                    "monetaryValue", amount(benefit.group(1))));
        }
        return terms;
    }

    /** Находит зачёркнутую цену до скидки, если она выше текущей. */
    private static BigDecimal regularPrice(Element card, BigDecimal currentPrice) {
        BigDecimal maximum = null;
        for (Element paragraph : card.select("p")) {
            Matcher matcher = MONEY.matcher(paragraph.text().trim());
            if (matcher.matches()) {
                BigDecimal candidate = amount(matcher.group(1));
                if (candidate.compareTo(currentPrice) > 0 && (maximum == null || candidate.compareTo(maximum) > 0)) {
                    maximum = candidate;
                }
            }
        }
        return maximum;
    }

    /** Преобразует Schema.org availability в внутренний статус наличия. */
    private static AvailabilityStatus availability(Element element) {
        if (element == null) {
            return AvailabilityStatus.UNKNOWN;
        }
        String href = element.attr("href").toLowerCase(Locale.ROOT);
        if (href.endsWith("/instock")) {
            return AvailabilityStatus.IN_STOCK;
        }
        if (href.endsWith("/outofstock") || href.endsWith("/soldout")) {
            return AvailabilityStatus.OUT_OF_STOCK;
        }
        if (href.endsWith("/preorder")) {
            return AvailabilityStatus.PREORDER;
        }
        return AvailabilityStatus.UNKNOWN;
    }

    /** Преобразует Schema.org condition в новое или бывшее в употреблении состояние. */
    private static ConditionType condition(Element element) {
        if (element == null) {
            return ConditionType.NEW;
        }
        String href = element.attr("href").toLowerCase(Locale.ROOT);
        return href.endsWith("/usedcondition") ? ConditionType.USED : ConditionType.NEW;
    }

    /** Определяет Apple по нормализованному началу названия товара. */
    private static String brand(String name) {
        return name.toLowerCase(Locale.ROOT).startsWith("apple ") ? "Apple" : null;
    }

    /** Извлекает стабильный slug товара из пути его страницы. */
    private static String externalId(URI productUri) {
        Matcher matcher = PRODUCT_PATH.matcher(productUri.getPath());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Unexpected 941 product URL: " + productUri);
        }
        return matcher.group(1);
    }

    /** Читает обязательный атрибут элемента, используя видимый текст как запасной источник. */
    private static String requiredAttributeOrText(Element root, String selector, String attribute) {
        Element element = required(root, selector);
        String value = element.attr(attribute);
        return value.isBlank() ? element.text().trim() : value.trim();
    }

    /** Возвращает обязательный дочерний элемент или останавливает импорт повреждённой карточки. */
    private static Element required(Element root, String selector) {
        Element element = root.selectFirst(selector);
        if (element == null) {
            throw new IllegalArgumentException("941 product card is missing " + selector);
        }
        return element;
    }

    /** Нормализует отображаемую денежную строку в точное десятичное значение. */
    private static BigDecimal amount(String raw) {
        String normalized = raw.replaceAll("[^0-9.,]", "").replace(',', '.');
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Price is empty");
        }
        return new BigDecimal(normalized);
    }

    /** Разрешает адаптеру читать только официальный домен 941 и его поддомены. */
    private static void validateHost(URI uri) {
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (!host.equals("941store.ru") && !host.endsWith(".941store.ru")) {
            throw new IllegalArgumentException("941 connector cannot read another host");
        }
    }
}
