package com.pricehunter.offer;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface CollectionRunRepository extends JpaRepository<CollectionRun, UUID> {

    Slice<CollectionRun> findByMarketIdAndStartedAtBetweenOrderByStartedAtDesc(
            UUID marketId, Instant from, Instant to, Pageable pageable);
}
