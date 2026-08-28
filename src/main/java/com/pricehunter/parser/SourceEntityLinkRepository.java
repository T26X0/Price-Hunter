package com.pricehunter.parser;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Хранилище сопоставлений идентификаторов внешних источников и внутренних сущностей. */
public interface SourceEntityLinkRepository extends JpaRepository<SourceEntityLink, UUID> {

    /** Находит уже известную сущность без повторного нечёткого сравнения названий. */
    Optional<SourceEntityLink> findByParserSourceIdAndEntityTypeAndExternalId(
            UUID parserSourceId, SourceEntityType entityType, String externalId);
}
