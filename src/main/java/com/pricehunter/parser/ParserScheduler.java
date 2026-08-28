package com.pricehunter.parser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "pricehunter.parser.scheduling.enabled", havingValue = "true")
/**
 * Периодически выбирает просроченные источники небольшими порциями и запускает нужную работу.
 * Активируется только явной настройкой, поэтому новый проект не начинает обход сайтов самопроизвольно.
 */
public class ParserScheduler {

    private static final List<ParserJobStatus> ACTIVE = List.of(
            ParserJobStatus.QUEUED, ParserJobStatus.RUNNING);

    private final ParserSourceRepository sourceRepository;
    private final ParserJobRepository jobRepository;
    private final ParserJobLifecycleService lifecycleService;
    private final CatalogImportService catalogImportService;
    private final ParserSchedulePolicy schedulePolicy;

    /** Один проход планировщика: товары, цены, затем поиск новых магазинов. */
    @Scheduled(cron = "${pricehunter.parser.scheduling.poll-cron}")
    public void scheduleDueWork() {
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
