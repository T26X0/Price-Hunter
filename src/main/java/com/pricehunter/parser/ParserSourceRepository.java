package com.pricehunter.parser;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Оптимизированные запросы конфигураций источников и очередей планового обхода. */
public interface ParserSourceRepository extends JpaRepository<ParserSource, UUID> {

    /** Загружает источник вместе с городом и сетью, чтобы импорт не выполнял ленивые запросы по одному. */
    @EntityGraph(attributePaths = {"market", "market.city", "market.retailChain"})
    @Query("select source from ParserSource source where source.id = :sourceId")
    Optional<ParserSource> findWithMarketById(@Param("sourceId") UUID sourceId);

    /** Ищет точный дубль конфигурации источника в рамках рынка. */
    Optional<ParserSource> findBySourceTypeAndConnectorKeyAndBaseUrlAndMarketId(
            ParserSourceType sourceType, String connectorKey, String baseUrl, UUID marketId);

    @Query("""
            select source from ParserSource source
             where source.enabled = true
               and source.sourceType = com.pricehunter.parser.ParserSourceType.WEBSITE
               and (source.lastPriceScanAt is null or source.lastPriceScanAt <= :cutoff)
             order by source.lastPriceScanAt, source.id
            """)
    Slice<ParserSource> findDueForPriceScan(@Param("cutoff") Instant cutoff, Pageable pageable);

    @Query("""
            select source from ParserSource source
             where source.enabled = true
               and source.sourceType = com.pricehunter.parser.ParserSourceType.WEBSITE
               and (source.lastProductScanAt is null or source.lastProductScanAt <= :cutoff)
             order by source.lastProductScanAt, source.id
            """)
    Slice<ParserSource> findDueForProductScan(@Param("cutoff") Instant cutoff, Pageable pageable);

    @Query("""
            select source from ParserSource source
             where source.enabled = true
               and source.sourceType <> com.pricehunter.parser.ParserSourceType.WEBSITE
               and (source.lastStoreScanAt is null or source.lastStoreScanAt <= :cutoff)
             order by source.lastStoreScanAt, source.id
            """)
    Slice<ParserSource> findDueForStoreScan(@Param("cutoff") Instant cutoff, Pageable pageable);
}
    /** Возвращает ограниченную порцию сайтов, у которых пора обновить цены. */
    /** Возвращает ограниченную порцию сайтов, у которых пора искать новые товары. */
    /** Возвращает поисковые и картографические источники, у которых пора искать магазины. */
