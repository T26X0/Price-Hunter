package com.pricehunter.parser;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "pricehunter.parser.scheduling.enabled", havingValue = "true")
/**
 * Периодически выбирает просроченные источники небольшими порциями и запускает нужную работу.
 * Активируется только явной настройкой, поэтому новый проект не начинает обход сайтов самопроизвольно.
 */
public class ParserScheduler {

    private final ParserDueWorkService dueWorkService;

    /** Один проход планировщика: товары, цены, затем поиск новых магазинов. */
    @Scheduled(cron = "${pricehunter.parser.scheduling.poll-cron}")
    public void scheduleDueWork() {
        dueWorkService.runDueWork();
    }
}
