package com.pricehunter.parser;

import com.pricehunter.parser.connector.CatalogConnector;
import com.pricehunter.parser.connector.CatalogScanRequest;
import com.pricehunter.parser.connector.CatalogScanResult;
import com.pricehunter.parser.connector.ConnectorRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
/** Оркестратор полного запуска каталога: задание, коннектор, сохранение и финальный статус. */
public class CatalogImportService {

    private final ParserSourceRepository sourceRepository;
    private final ParserJobRepository jobRepository;
    private final ConnectorRegistry connectorRegistry;
    private final ParserJobLifecycleService lifecycleService;
    private final CatalogPersistenceService persistenceService;

    /**
     * Выполняет один импорт каталога и гарантированно завершает созданное задание успехом или ошибкой.
     *
     * @return идентификатор задания, по которому можно прочитать статистику
     */
    public UUID run(UUID sourceId, ParserJobType jobType, URI catalogUri, String cityKey, Instant observedAt) {
        if (jobType != ParserJobType.PRICE_REFRESH && jobType != ParserJobType.PRODUCT_DISCOVERY) {
            throw new IllegalArgumentException("Catalog import supports price or product jobs only");
        }
        ParserSource source = sourceRepository.findWithMarketById(sourceId).orElseThrow();
        if (!source.isEnabled() || source.getMarket() == null) {
            throw new IllegalStateException("Parser source is disabled or has no market");
        }
        verifySameOrigin(source.getBaseUrl(), catalogUri);

        UUID jobId = lifecycleService.queue(sourceId, jobType, observedAt);
        lifecycleService.start(jobId, observedAt);
        try {
            CatalogConnector connector = connectorRegistry.catalog(
                    source.getSourceType(), source.getConnectorMode(), source.getConnectorKey());
            CatalogScanResult scan = connector.scan(new CatalogScanRequest(
                    sourceId, source.getMarket().getId(), catalogUri, cityKey, observedAt));
            ParserJobOutcome outcome = persistenceService.persist(sourceId, jobId, scan);
            ParserJobStatus status = terminalStatus(outcome);
            lifecycleService.complete(jobId, status, outcome, null, Instant.now());
            return jobId;
        } catch (RuntimeException exception) {
            lifecycleService.complete(jobId, ParserJobStatus.FAILED,
                    ParserJobOutcome.empty(), concise(exception), Instant.now());
            throw exception;
        }
    }

    /** Возвращает сохранённое задание с его статусом и итоговыми счётчиками. */
    public ParserJob result(UUID jobId) {
        return jobRepository.findById(jobId).orElseThrow();
    }

    /** Выбирает терминальный статус по количеству ошибок и кандидатов ручной проверки. */
    private static ParserJobStatus terminalStatus(ParserJobOutcome outcome) {
        if (outcome.errorCount() > 0) {
            return outcome.foundCount() > outcome.errorCount()
                    ? ParserJobStatus.PARTIALLY_SUCCEEDED
                    : ParserJobStatus.FAILED;
        }
        return outcome.reviewCount() > 0 ? ParserJobStatus.NEEDS_REVIEW : ParserJobStatus.SUCCEEDED;
    }

    /** Запрещает подменить настроенный домен источника произвольным адресом. */
    private static void verifySameOrigin(String configuredBaseUrl, URI requestedUri) {
        URI base = URI.create(configuredBaseUrl);
        int basePort = base.getPort() == -1 ? 443 : base.getPort();
        int requestedPort = requestedUri.getPort() == -1 ? 443 : requestedUri.getPort();
        if (!"https".equalsIgnoreCase(requestedUri.getScheme())
                || base.getHost() == null
                || !base.getHost().equalsIgnoreCase(requestedUri.getHost())
                || basePort != requestedPort) {
            throw new IllegalArgumentException("Catalog URL must use the configured parser source origin");
        }
    }

    /** Сокращает техническую ошибку до размера колонки в базе. */
    private static String concise(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() <= 1900 ? message : message.substring(0, 1900);
    }
}
