package com.pricehunter.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Доступ к справочнику характеристик товара. */
public interface AttributeDefinitionRepository extends JpaRepository<AttributeDefinition, UUID> {

    /** Находит определение по стабильному коду. */
    Optional<AttributeDefinition> findByCode(String code);

    /** Загружает набор определений одним запросом. */
    List<AttributeDefinition> findByCodeIn(Collection<String> codes);
}
