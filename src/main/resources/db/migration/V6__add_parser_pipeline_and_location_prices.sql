ALTER TABLE offers
    ADD COLUMN store_location_id UUID;

ALTER TABLE offers
    ADD CONSTRAINT fk_offers_store_market
        FOREIGN KEY (store_location_id, market_id)
        REFERENCES store_locations (id, market_id)
        ON DELETE CASCADE;

ALTER TABLE offers DROP CONSTRAINT uq_offers_market_key;
DROP INDEX uq_offers_market_external_id;

CREATE UNIQUE INDEX uq_offers_market_key
    ON offers (market_id, offer_key)
    WHERE store_location_id IS NULL;
CREATE UNIQUE INDEX uq_offers_location_key
    ON offers (store_location_id, offer_key)
    WHERE store_location_id IS NOT NULL;
CREATE UNIQUE INDEX uq_offers_market_external_id
    ON offers (market_id, external_offer_id)
    WHERE store_location_id IS NULL AND external_offer_id IS NOT NULL;
CREATE UNIQUE INDEX uq_offers_location_external_id
    ON offers (store_location_id, external_offer_id)
    WHERE store_location_id IS NOT NULL AND external_offer_id IS NOT NULL;

DROP INDEX idx_offers_variant_market_current;
DROP INDEX idx_offers_market_parser_lookup;

CREATE INDEX idx_offers_variant_market_current
    ON offers (product_variant_id, market_id, store_location_id, condition_type, id)
    INCLUDE (regular_price, sale_price, conditional_price, currency, availability_status, product_url, last_checked_at)
    WHERE active;
CREATE INDEX idx_offers_market_parser_lookup
    ON offers (market_id, store_location_id, external_offer_id, offer_key)
    INCLUDE (id, state_hash, last_checked_at, last_seen_at)
    WHERE active;

CREATE TABLE parser_sources (
    id UUID PRIMARY KEY,
    market_id UUID REFERENCES chain_city_markets (id) ON DELETE CASCADE,
    source_type VARCHAR(30) NOT NULL,
    connector_mode VARCHAR(20) NOT NULL,
    connector_key VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    base_url VARCHAR(1000) NOT NULL,
    configuration JSONB NOT NULL DEFAULT '{}'::jsonb,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_price_scan_at TIMESTAMPTZ,
    last_product_scan_at TIMESTAMPTZ,
    last_store_scan_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_parser_sources_type CHECK (
        source_type IN ('YANDEX_SEARCH', 'GOOGLE_SEARCH', 'YANDEX_MAPS', 'TWO_GIS', 'WEBSITE')
    ),
    CONSTRAINT ck_parser_sources_mode CHECK (connector_mode IN ('HTML', 'BROWSER', 'API')),
    CONSTRAINT ck_parser_sources_key_not_blank CHECK (BTRIM(connector_key) <> ''),
    CONSTRAINT ck_parser_sources_url_not_blank CHECK (BTRIM(base_url) <> '')
);

CREATE UNIQUE INDEX uq_parser_sources_identity
    ON parser_sources (
        source_type,
        connector_key,
        base_url,
        COALESCE(market_id, '00000000-0000-0000-0000-000000000000'::uuid)
    );
CREATE INDEX idx_parser_sources_price_due
    ON parser_sources (last_price_scan_at, id)
    WHERE enabled AND source_type = 'WEBSITE';
CREATE INDEX idx_parser_sources_product_due
    ON parser_sources (last_product_scan_at, id)
    WHERE enabled AND source_type = 'WEBSITE';
CREATE INDEX idx_parser_sources_store_due
    ON parser_sources (last_store_scan_at, source_type, id)
    WHERE enabled AND source_type IN ('YANDEX_SEARCH', 'GOOGLE_SEARCH', 'YANDEX_MAPS', 'TWO_GIS');

CREATE TABLE parser_jobs (
    id UUID PRIMARY KEY,
    parser_source_id UUID NOT NULL REFERENCES parser_sources (id) ON DELETE CASCADE,
    market_id UUID REFERENCES chain_city_markets (id) ON DELETE CASCADE,
    collection_run_id UUID REFERENCES collection_runs (id) ON DELETE SET NULL,
    job_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    scheduled_at TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    attempt_count SMALLINT NOT NULL DEFAULT 0,
    max_attempts SMALLINT NOT NULL DEFAULT 3,
    found_count INTEGER NOT NULL DEFAULT 0,
    created_count INTEGER NOT NULL DEFAULT 0,
    changed_count INTEGER NOT NULL DEFAULT 0,
    review_count INTEGER NOT NULL DEFAULT 0,
    error_count INTEGER NOT NULL DEFAULT 0,
    error_summary VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_parser_jobs_type CHECK (
        job_type IN ('PRICE_REFRESH', 'PRODUCT_DISCOVERY', 'STORE_DISCOVERY')
    ),
    CONSTRAINT ck_parser_jobs_status CHECK (
        status IN ('QUEUED', 'RUNNING', 'SUCCEEDED', 'PARTIALLY_SUCCEEDED', 'FAILED', 'NEEDS_REVIEW')
    ),
    CONSTRAINT ck_parser_jobs_attempts CHECK (
        attempt_count >= 0 AND max_attempts > 0 AND attempt_count <= max_attempts
    ),
    CONSTRAINT ck_parser_jobs_counts CHECK (
        found_count >= 0 AND created_count >= 0 AND changed_count >= 0
        AND review_count >= 0 AND error_count >= 0
    ),
    CONSTRAINT uq_parser_jobs_schedule UNIQUE (parser_source_id, job_type, scheduled_at)
);

CREATE INDEX idx_parser_jobs_queue
    ON parser_jobs (status, scheduled_at, id)
    WHERE status IN ('QUEUED', 'RUNNING');
CREATE INDEX idx_parser_jobs_source_recent
    ON parser_jobs (parser_source_id, job_type, scheduled_at DESC);

CREATE TABLE review_candidates (
    id UUID PRIMARY KEY,
    parser_source_id UUID NOT NULL REFERENCES parser_sources (id) ON DELETE CASCADE,
    parser_job_id UUID REFERENCES parser_jobs (id) ON DELETE SET NULL,
    review_type VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    fingerprint VARCHAR(64) NOT NULL,
    candidate_name VARCHAR(500) NOT NULL,
    candidate_category VARCHAR(150),
    source_url VARCHAR(2000),
    external_id VARCHAR(500),
    suggested_entity_id UUID,
    confidence NUMERIC(5, 4),
    reason VARCHAR(1000) NOT NULL,
    raw_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    first_seen_at TIMESTAMPTZ NOT NULL,
    last_seen_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_review_candidates_type CHECK (
        review_type IN ('STORE_MATCH', 'PRODUCT_MATCH', 'CATEGORY_CLASSIFICATION')
    ),
    CONSTRAINT ck_review_candidates_status CHECK (
        status IN ('PENDING', 'APPROVED', 'MERGED', 'REJECTED')
    ),
    CONSTRAINT ck_review_candidates_confidence CHECK (
        confidence IS NULL OR confidence BETWEEN 0 AND 1
    ),
    CONSTRAINT uq_review_candidates_fingerprint UNIQUE (parser_source_id, review_type, fingerprint)
);

CREATE INDEX idx_review_candidates_pending
    ON review_candidates (review_type, first_seen_at, id)
    WHERE status = 'PENDING';
CREATE INDEX idx_review_candidates_suggestion
    ON review_candidates (suggested_entity_id, review_type)
    WHERE suggested_entity_id IS NOT NULL AND status = 'PENDING';
CREATE INDEX idx_review_candidates_payload_gin
    ON review_candidates USING GIN (raw_payload);

CREATE TABLE source_entity_links (
    id UUID PRIMARY KEY,
    parser_source_id UUID NOT NULL REFERENCES parser_sources (id) ON DELETE CASCADE,
    entity_type VARCHAR(30) NOT NULL,
    external_id VARCHAR(500) NOT NULL,
    internal_entity_id UUID NOT NULL,
    source_url VARCHAR(2000),
    fingerprint VARCHAR(64) NOT NULL,
    first_seen_at TIMESTAMPTZ NOT NULL,
    last_seen_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_source_entity_links_type CHECK (
        entity_type IN ('RETAIL_CHAIN', 'STORE_LOCATION', 'PRODUCT_MODEL', 'PRODUCT_VARIANT', 'OFFER')
    ),
    CONSTRAINT uq_source_entity_links_external UNIQUE (parser_source_id, entity_type, external_id)
);

CREATE INDEX idx_source_entity_links_internal
    ON source_entity_links (entity_type, internal_entity_id, parser_source_id);
CREATE INDEX idx_source_entity_links_fingerprint
    ON source_entity_links (entity_type, fingerprint);

COMMENT ON COLUMN offers.store_location_id IS
    'Optional branch price scope. NULL means a city-wide market offer; non-NULL means the price belongs to one branch.';
COMMENT ON TABLE offers IS
    'Current offer state scoped either to a city market or, when store_location_id is set, to one physical branch.';
COMMENT ON TABLE parser_sources IS
    'Configurable parser endpoints. connector_mode allows HTML/browser implementations to be replaced by APIs.';
COMMENT ON TABLE parser_jobs IS
    'Idempotent scheduled parser work: daily prices, weekly products, and biweekly store discovery.';
COMMENT ON TABLE review_candidates IS
    'Ambiguous stores, products, and non-technical catalog items awaiting manual review.';
COMMENT ON TABLE source_entity_links IS
    'Stable mapping from source identifiers to canonical Price Hunter entities for duplicate prevention.';
