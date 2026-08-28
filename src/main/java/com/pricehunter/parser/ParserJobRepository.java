package com.pricehunter.parser;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.Collection;
import java.util.UUID;

/** Запросы очереди заданий парсера и блокировки их жизненного цикла. */
public interface ParserJobRepository extends JpaRepository<ParserJob, UUID> {

    /** Проверяет, есть ли у источника уже поставленное или выполняющееся задание этого типа. */
    boolean existsByParserSourceIdAndJobTypeAndStatusIn(
            UUID parserSourceId, ParserJobType jobType, Collection<ParserJobStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select job from ParserJob job where job.id = :jobId")
    Optional<ParserJob> findForUpdate(@Param("jobId") UUID jobId);

    @Query("""
            select job from ParserJob job
             where job.status = com.pricehunter.parser.ParserJobStatus.QUEUED
               and job.scheduledAt <= :now
             order by job.scheduledAt, job.id
            """)
    Slice<ParserJob> findReady(@Param("now") Instant now, Pageable pageable);
}
    /** Загружает задание с блокировкой строки, исключая одновременный переход статуса. */
    /** Возвращает порцию заданий, время запуска которых уже наступило. */
