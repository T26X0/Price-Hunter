package com.pricehunter.admin;

import com.pricehunter.shared.ApiRoutes;
import com.pricehunter.shared.SliceResponse;
import com.pricehunter.parser.ParserDueWorkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Минимальный административный API для настройки первого источника и безопасного ручного запуска. */
@RestController
@RequestMapping(ApiRoutes.Admin.ROOT)
@RequiredArgsConstructor
@Validated
@Tag(name = "Admin", description = "Настройка справочников и управляемые ручные запуски парсеров")
public class AdminController {

    private final AdminConfigurationService adminService;
    private final AdminReviewService reviewService;
    private final ParserDueWorkService parserDueWorkService;

    /** Создаёт город для будущих рынков и фильтров каталога. */
    @PostMapping(ApiRoutes.Admin.CITIES)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать город")
    public AdminCreatedResponse createCity(@Valid @RequestBody CreateCityRequest request) {
        return adminService.createCity(request);
    }

    /** Создаёт торговую сеть, например 941. */
    @PostMapping(ApiRoutes.Admin.CHAINS)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать торговую сеть")
    public AdminCreatedResponse createChain(@Valid @RequestBody CreateRetailChainRequest request) {
        return adminService.createChain(request);
    }

    /** Создаёт рынок сети в выбранном городе и канале продаж. */
    @PostMapping(ApiRoutes.Admin.MARKETS)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать рынок сети в городе")
    public AdminCreatedResponse createMarket(@Valid @RequestBody CreateMarketRequest request) {
        return adminService.createMarket(request);
    }

    /** Сохраняет источник парсинга до его первого запуска. */
    @PostMapping(ApiRoutes.Admin.PARSER_SOURCES)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать источник парсинга")
    public ParserSourceResponse createSource(@Valid @RequestBody CreateParserSourceRequest request) {
        return adminService.createSource(request);
    }

    /** Возвращает настроенные источники постранично. */
    @GetMapping(ApiRoutes.Admin.PARSER_SOURCES)
    @Operation(summary = "Список источников парсинга")
    public SliceResponse<ParserSourceResponse> findSources(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return adminService.findSources(page, size);
    }

    /** Включает или отключает источник без потери конфигурации и истории заданий. */
    @PatchMapping(ApiRoutes.Admin.PARSER_SOURCES + "/{sourceId}/enabled")
    @Operation(summary = "Изменить доступность источника")
    public ParserSourceResponse updateSourceEnabled(@PathVariable UUID sourceId,
                                                     @Valid @RequestBody SourceEnabledRequest request) {
        return adminService.updateSourceEnabled(sourceId, request.enabled());
    }

    /** Запускает каталог по URL сохранённого источника и возвращает финальный job DTO. */
    @PostMapping(ApiRoutes.Admin.PARSER_SOURCES + "/{sourceId}/runs")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Запустить источник вручную")
    public ParserJobResponse runSource(@PathVariable UUID sourceId,
                                       @Valid @RequestBody ManualParserRunRequest request) {
        return adminService.runSource(sourceId, request.type());
    }

    /** Возвращает итог одного задания, включая счётчики и техническую ошибку при неудаче. */
    @GetMapping(ApiRoutes.Admin.PARSER_JOBS + "/{jobId}")
    @Operation(summary = "Результат задания парсера")
    public ParserJobResponse findJob(@PathVariable UUID jobId) {
        return adminService.findJob(jobId);
    }

    /** Возвращает историю запусков одного источника. */
    @GetMapping(ApiRoutes.Admin.PARSER_SOURCES + "/{sourceId}/jobs")
    @Operation(summary = "История заданий источника")
    public SliceResponse<ParserJobResponse> findJobs(@PathVariable UUID sourceId,
                                                      @RequestParam(defaultValue = "0") @Min(0) int page,
                                                      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return adminService.findJobs(sourceId, page, size);
    }

    /** Выполняет один проход по просроченным источникам для внешнего production-планировщика. */
    @PostMapping(ApiRoutes.Admin.PARSER_SCHEDULER_RUN)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Запустить плановый проход парсеров")
    public void runScheduledParsers() {
        parserDueWorkService.runDueWork();
    }

    /** Возвращает кандидатов, которые ожидают ручного решения или были обработаны ранее. */
    @GetMapping(ApiRoutes.Admin.REVIEW_CANDIDATES)
    @Operation(summary = "Очередь ручной проверки")
    public SliceResponse<ReviewCandidateResponse> findReviewCandidates(
            @RequestParam(defaultValue = "PENDING") com.pricehunter.parser.ReviewStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return reviewService.findCandidates(status, page, size);
    }

    /** Возвращает одну карточку ручной проверки по идентификатору. */
    @GetMapping(ApiRoutes.Admin.REVIEW_CANDIDATES + "/{candidateId}")
    @Operation(summary = "Карточка кандидата ручной проверки")
    public ReviewCandidateResponse findReviewCandidate(@PathVariable UUID candidateId) {
        return reviewService.findCandidate(candidateId);
    }

    /** Применяет безопасное решение оператора по кандидату без фиктивного импорта товара или категории. */
    @PostMapping(ApiRoutes.Admin.REVIEW_CANDIDATES + "/{candidateId}/resolve")
    @Operation(summary = "Разрешить кандидат ручной проверки")
    public ReviewCandidateResponse resolveReviewCandidate(@PathVariable UUID candidateId,
                                                          @Valid @RequestBody ResolveReviewCandidateRequest request) {
        return reviewService.resolve(candidateId, request);
    }
}
