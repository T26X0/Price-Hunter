package com.pricehunter.admin;

import com.pricehunter.city.City;
import com.pricehunter.city.CityRepository;
import com.pricehunter.parser.CatalogImportService;
import com.pricehunter.parser.ParserJob;
import com.pricehunter.parser.ParserJobRepository;
import com.pricehunter.parser.ParserJobType;
import com.pricehunter.parser.ParserSource;
import com.pricehunter.parser.ParserSourceRepository;
import com.pricehunter.retail.ChainCityMarket;
import com.pricehunter.retail.ChainCityMarketRepository;
import com.pricehunter.retail.RetailChain;
import com.pricehunter.retail.RetailChainRepository;
import com.pricehunter.shared.ConflictException;
import com.pricehunter.shared.NotFoundException;
import com.pricehunter.shared.SliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Минимальный административный слой: создаёт только необходимые для vertical slice настройки
 * и запускает уже сохранённый источник без принятия внешнего URL из HTTP-запроса запуска.
 */
@Service
@RequiredArgsConstructor
public class AdminConfigurationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final CityRepository cityRepository;
    private final RetailChainRepository chainRepository;
    private final ChainCityMarketRepository marketRepository;
    private final ParserSourceRepository sourceRepository;
    private final ParserJobRepository jobRepository;
    private final CatalogImportService catalogImportService;

    /** Создаёт город, если страна и нормализованное название ещё не заняты. */
    @Transactional
    public AdminCreatedResponse createCity(CreateCityRequest request) {
        String countryCode = request.countryCode() == null || request.countryCode().isBlank()
                ? "RU" : request.countryCode().trim().toUpperCase(Locale.ROOT);
        String normalizedName = request.name().trim().toLowerCase(Locale.ROOT);
        if (cityRepository.findByCountryCodeAndNormalizedName(countryCode, normalizedName).isPresent()) {
            throw new ConflictException("City '" + request.name().trim() + "' already exists");
        }
        City city = cityRepository.save(new City(request.name(), countryCode, request.timezone()));
        return new AdminCreatedResponse(city.getId());
    }

    /** Создаёт торговую сеть с уникальным кодом. */
    @Transactional
    public AdminCreatedResponse createChain(CreateRetailChainRequest request) {
        if (chainRepository.existsByCodeIgnoreCase(request.code().trim())) {
            throw new ConflictException("Retail chain code '" + request.code().trim() + "' already exists");
        }
        RetailChain chain = chainRepository.save(new RetailChain(request.name(), request.code(), request.websiteUrl(),
                request.parserType(), request.parserEnabled() == null || request.parserEnabled()));
        return new AdminCreatedResponse(chain.getId());
    }

    /** Создаёт уникальное коммерческое присутствие сети в одном городе и канале продаж. */
    @Transactional
    public AdminCreatedResponse createMarket(CreateMarketRequest request) {
        RetailChain chain = chainRepository.findById(request.chainId())
                .orElseThrow(() -> new NotFoundException("Retail chain '" + request.chainId() + "' was not found"));
        City city = cityRepository.findById(request.cityId())
                .orElseThrow(() -> new NotFoundException("City '" + request.cityId() + "' was not found"));
        if (marketRepository.findByRetailChainIdAndCityIdAndSalesChannel(chain.getId(), city.getId(),
                request.salesChannel()).isPresent()) {
            throw new ConflictException("Market for this chain, city and sales channel already exists");
        }
        ChainCityMarket market = marketRepository.save(new ChainCityMarket(chain, city, request.salesChannel(),
                request.externalMarketId(), request.sourceBaseUrl(),
                request.parserEnabled() == null || request.parserEnabled()));
        return new AdminCreatedResponse(market.getId());
    }

    /** Сохраняет источник до запуска и предотвращает точный дубль конфигурации. */
    @Transactional
    public ParserSourceResponse createSource(CreateParserSourceRequest request) {
        validateSourceUrl(request.baseUrl());
        ChainCityMarket market = marketRepository.findById(request.marketId())
                .orElseThrow(() -> new NotFoundException("Market '" + request.marketId() + "' was not found"));
        if (sourceRepository.findBySourceTypeAndConnectorKeyAndBaseUrlAndMarketId(request.sourceType(),
                request.connectorKey().trim().toLowerCase(Locale.ROOT), request.baseUrl().trim(), market.getId())
                .isPresent()) {
            throw new ConflictException("The same parser source already exists for this market");
        }
        Map<String, Object> configuration = request.configuration() == null ? Map.of() : request.configuration();
        ParserSource source = sourceRepository.save(new ParserSource(market, request.sourceType(), request.connectorMode(),
                request.connectorKey(), request.name(), request.baseUrl(), configuration));
        source.setEnabled(request.enabled() == null || request.enabled());
        return ParserSourceResponse.from(source);
    }

    /** Возвращает источники небольшими порциями для административного интерфейса. */
    @Transactional(readOnly = true)
    public SliceResponse<ParserSourceResponse> findSources(int page, int size) {
        var sources = sourceRepository.findAllWithMarket(page(page, size));
        return SliceResponse.from(sources, ParserSourceResponse::from);
    }

    /** Включает либо выключает источник, не удаляя его задания, данные или историю. */
    @Transactional
    public ParserSourceResponse updateSourceEnabled(UUID sourceId, boolean enabled) {
        ParserSource source = sourceRepository.findWithMarketById(sourceId)
                .orElseThrow(() -> new NotFoundException("Parser source '" + sourceId + "' was not found"));
        source.setEnabled(enabled);
        return ParserSourceResponse.from(source);
    }

    /**
     * Запускает только ранее сохранённый WEBSITE-каталог и передаёт коннектору URL источника.
     * HTTP-клиент запуска не может подменить домен или произвольный адрес.
     */
    public ParserJobResponse runSource(UUID sourceId, ParserJobType type) {
        if (type == ParserJobType.STORE_DISCOVERY) {
            throw new IllegalArgumentException("Manual catalog run supports PRICE_REFRESH or PRODUCT_DISCOVERY only");
        }
        ParserSource source = sourceRepository.findWithMarketById(sourceId)
                .orElseThrow(() -> new NotFoundException("Parser source '" + sourceId + "' was not found"));
        if (!source.isEnabled()) {
            throw new IllegalArgumentException("Parser source is disabled");
        }
        if (source.getMarket() == null) {
            throw new IllegalArgumentException("Parser source must be assigned to a market");
        }
        String cityKey = String.valueOf(source.getConfiguration().getOrDefault("cityKey",
                source.getMarket().getCity().getNormalizedName()));
        UUID jobId = catalogImportService.runManually(sourceId, type, URI.create(source.getBaseUrl()), cityKey, Instant.now());
        return findJob(jobId);
    }

    /** Возвращает результат одного задания с финальными счётчиками. */
    @Transactional(readOnly = true)
    public ParserJobResponse findJob(UUID jobId) {
        ParserJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Parser job '" + jobId + "' was not found"));
        return ParserJobResponse.from(job);
    }

    /** Возвращает недавнюю историю заданий выбранного источника. */
    @Transactional(readOnly = true)
    public SliceResponse<ParserJobResponse> findJobs(UUID sourceId, int page, int size) {
        if (!sourceRepository.existsById(sourceId)) {
            throw new NotFoundException("Parser source '" + sourceId + "' was not found");
        }
        return SliceResponse.from(jobRepository.findByParserSourceIdOrderByScheduledAtDescIdDesc(sourceId,
                page(page, size)), ParserJobResponse::from);
    }

    /** Ограничивает административные списки до безопасного размера. */
    private static PageRequest page(int page, int size) {
        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("page must be non-negative and size must be positive");
        }
        return PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
    }

    /** Рано отвергает небезопасный и нерабочий URL ещё до записи источника в базу. */
    private static void validateSourceUrl(String rawUrl) {
        URI uri;
        try {
            uri = URI.create(rawUrl.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("baseUrl must be a valid HTTPS URL");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("baseUrl must be a public HTTPS URL");
        }
    }
}
