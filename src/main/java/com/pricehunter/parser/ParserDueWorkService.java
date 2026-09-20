package com.pricehunter.parser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.util.List;

/**
 * Выполняет один ограниченный проход по источникам, у которых наступил срок обновления.
 * Сервис не зависит от механизма запуска: его вызывает локальный Spring-планировщик или внешний Cloudflare Cron.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ParserDueWorkService {

    private static final List<ParserJobStatus> ACTIVE = List.of(
            ParserJobStatus.QUEUED, ParserJobStatus.RUNNING);

    private final ParserSourceRepository sourceRepository;
    private final ParserJobRepository jobRepository;
    private final ParserJobLifecycleService lifecycleService;
    private final CatalogImportService catalogImportService;
    private final ParserSchedulePolicy schedulePolicy;

    /** Запускает просроченный поиск товаров, обновление цен и поиск магазинов порциями до 25 источников. */
    public void runDueWork() {
        Instant now = Instant.now();
        runWebsiteSources(ParserJobType.PRODUCT_DISCOVERY,
                sourceRepository.findDueForProductScan(
                        schedulePolicy.cutoff(ParserJobType.PRODUCT_DISCOVERY, now), PageRequest.of(0, 25)), now);
        runWebsiteSources(ParserJobType.PRICE_REFRESH,
                sourceRepository.findDueForPriceScan(
                        schedulePolicy.cutoff(ParserJobType.PRICE_REFRESH, now), PageRequest.of(0, 25)), now);
        queueStoreDiscovery(sourceRepository.findDueForStoreScan(
                schedulePolicy.cutoff(ParserJobType.STORE_DISCOVERY, now), PageRequest.of(0, 25)), now);
    }

    /** Последовательно запускает сайты, изолируя ошибку одного источника от остальных. */
    private void runWebsiteSources(ParserJobType jobType, Slice<ParserSource> sources, Instant now) {
        for (ParserSource source : sources) {
            if (hasActiveJob(source, jobType)) {
                continue;
            }
            String cityKey = String.valueOf(source.getConfiguration()
                    .getOrDefault("cityKey", "ekaterinburg"));
            try {
                catalogImportService.run(source.getId(), jobType,
                        URI.create(source.getBaseUrl()), cityKey, now);
            } catch (RuntimeException exception) {
                log.error("Parser job failed for source {} and type {}", source.getId(), jobType, exception);
            }
        }
    }

    /** Ставит задания обнаружения магазинов; исполнители карт и поисковиков подключаются отдельно. */
    private void queueStoreDiscovery(Slice<ParserSource> sources, Instant now) {
        for (ParserSource source : sources) {
            if (!hasActiveJob(source, ParserJobType.STORE_DISCOVERY)) {
                lifecycleService.queue(source.getId(), ParserJobType.STORE_DISCOVERY, now);
            }
        }
    }

    /** Проверяет защиту от параллельного дубля задания того же типа. */
    private boolean hasActiveJob(ParserSource source, ParserJobType jobType) {
        return jobRepository.existsByParserSourceIdAndJobTypeAndStatusIn(source.getId(), jobType, ACTIVE);
    }
}
