package com.pricehunter.retail;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RetailChainRepository extends JpaRepository<RetailChain, UUID> {

    Optional<RetailChain> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);
}
