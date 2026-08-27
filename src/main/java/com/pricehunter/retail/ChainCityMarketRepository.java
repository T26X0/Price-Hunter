package com.pricehunter.retail;

import com.pricehunter.query.ParserMarketTargetProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ChainCityMarketRepository extends JpaRepository<ChainCityMarket, UUID> {

    Optional<ChainCityMarket> findByRetailChainIdAndCityIdAndSalesChannel(
            UUID retailChainId, UUID cityId, SalesChannel salesChannel);

    @Query("""
            select m.id as marketId,
                   chain.id as chainId,
                   chain.code as chainCode,
                   chain.parserType as parserType,
                   city.id as cityId,
                   city.name as cityName,
                   m.salesChannel as salesChannel,
                   m.sourceBaseUrl as sourceBaseUrl
              from ChainCityMarket m
              join m.retailChain chain
              join m.city city
             where m.parserEnabled = true
               and chain.parserEnabled = true
               and (:parserType is null or chain.parserType = :parserType)
             order by chain.id, city.id, m.id
            """)
    Slice<ParserMarketTargetProjection> findParserTargets(
            @Param("parserType") com.pricehunter.store.ParserType parserType,
            Pageable pageable);
}
