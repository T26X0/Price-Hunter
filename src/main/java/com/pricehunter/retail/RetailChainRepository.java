package com.pricehunter.retail;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Доступ к торговым сетям и проверки уникального бизнес-кода. */
public interface RetailChainRepository extends JpaRepository<RetailChain, UUID> {

    /** Находит сеть по коду без учёта регистра. */
    Optional<RetailChain> findByCodeIgnoreCase(String code);

    /** Быстро проверяет занятость кода без загрузки сущности. */
    boolean existsByCodeIgnoreCase(String code);
}
