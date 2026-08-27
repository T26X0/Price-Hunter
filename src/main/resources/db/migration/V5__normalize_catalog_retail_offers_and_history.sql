ALTER TABLE prices RENAME TO legacy_prices;
ALTER TABLE stores RENAME TO legacy_stores;
ALTER TABLE products RENAME TO legacy_products;

ALTER TABLE cities
    ADD COLUMN normalized_name VARCHAR(200),
    ADD COLUMN country_code VARCHAR(2) NOT NULL DEFAULT 'RU',
    ADD COLUMN timezone VARCHAR(100),
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

UPDATE cities
SET normalized_name = LOWER(BTRIM(name));

ALTER TABLE cities
    ALTER COLUMN normalized_name SET NOT NULL;

CREATE UNIQUE INDEX uq_cities_country_normalized_name
    ON cities (country_code, normalized_name);

CREATE TABLE retail_chains (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    code VARCHAR(100) NOT NULL,
    website_url VARCHAR(1000),
    parser_type VARCHAR(100),
    parser_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_retail_chains_code_not_blank CHECK (BTRIM(code) <> '')
);

CREATE UNIQUE INDEX uq_retail_chains_code_ci ON retail_chains (LOWER(code));
CREATE UNIQUE INDEX uq_retail_chains_name_ci ON retail_chains (LOWER(name));
CREATE INDEX idx_retail_chains_parser_enabled
    ON retail_chains (id)
    WHERE parser_enabled;

CREATE TABLE chain_city_markets (
    id UUID PRIMARY KEY,
    retail_chain_id UUID NOT NULL REFERENCES retail_chains (id) ON DELETE CASCADE,
    city_id UUID NOT NULL REFERENCES cities (id) ON DELETE RESTRICT,
    sales_channel VARCHAR(30) NOT NULL,
    external_market_id VARCHAR(200),
    source_base_url VARCHAR(1000),
    parser_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_markets_sales_channel CHECK (sales_channel IN ('PHYSICAL', 'ONLINE')),
    CONSTRAINT uq_markets_chain_city_channel UNIQUE (retail_chain_id, city_id, sales_channel),
    CONSTRAINT uq_markets_id_chain UNIQUE (id, retail_chain_id)
);

CREATE INDEX idx_markets_parser_scan
    ON chain_city_markets (retail_chain_id, city_id, id)
    WHERE parser_enabled;
CREATE INDEX idx_markets_city_channel
    ON chain_city_markets (city_id, sales_channel, retail_chain_id);

CREATE TABLE store_locations (
    id UUID PRIMARY KEY,
    retail_chain_id UUID NOT NULL REFERENCES retail_chains (id) ON DELETE CASCADE,
    market_id UUID NOT NULL,
    external_store_id VARCHAR(300) NOT NULL,
    name VARCHAR(200) NOT NULL,
    address VARCHAR(500),
    latitude NUMERIC(9, 6),
    longitude NUMERIC(9, 6),
    website_url VARCHAR(1000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_store_locations_market_chain
        FOREIGN KEY (market_id, retail_chain_id)
        REFERENCES chain_city_markets (id, retail_chain_id)
        ON DELETE CASCADE,
    CONSTRAINT uq_store_locations_chain_external UNIQUE (retail_chain_id, external_store_id),
    CONSTRAINT uq_store_locations_id_market UNIQUE (id, market_id),
    CONSTRAINT ck_store_locations_latitude CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_store_locations_longitude CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180)
);

CREATE INDEX idx_store_locations_market_active
    ON store_locations (market_id, id)
    WHERE active;
CREATE INDEX idx_store_locations_chain_name
    ON store_locations (retail_chain_id, name);

CREATE TABLE product_models (
    id UUID PRIMARY KEY,
    brand VARCHAR(120),
    name VARCHAR(200) NOT NULL,
    catalog_key VARCHAR(160) NOT NULL,
    category_code VARCHAR(100),
    description VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_product_models_catalog_key_not_blank CHECK (BTRIM(catalog_key) <> '')
);

CREATE UNIQUE INDEX uq_product_models_catalog_key_ci
    ON product_models (LOWER(catalog_key));
CREATE INDEX idx_product_models_category_name
    ON product_models (category_code, name, id);

CREATE TABLE product_variants (
    id UUID PRIMARY KEY,
    product_model_id UUID NOT NULL REFERENCES product_models (id) ON DELETE CASCADE,
    canonical_key VARCHAR(500) NOT NULL,
    manufacturer_sku VARCHAR(160),
    display_name VARCHAR(300) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_product_variants_model_key UNIQUE (product_model_id, canonical_key),
    CONSTRAINT uq_product_variants_id_model UNIQUE (id, product_model_id)
);

CREATE INDEX idx_product_variants_model_display
    ON product_variants (product_model_id, display_name, id);
CREATE INDEX idx_product_variants_manufacturer_sku
    ON product_variants (manufacturer_sku)
    WHERE manufacturer_sku IS NOT NULL;

CREATE TABLE attribute_definitions (
    id UUID PRIMARY KEY,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    data_type VARCHAR(20) NOT NULL,
    unit VARCHAR(30),
    filterable BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_attribute_definitions_code UNIQUE (code),
    CONSTRAINT ck_attribute_definitions_data_type
        CHECK (data_type IN ('TEXT', 'NUMBER', 'BOOLEAN'))
);

CREATE TABLE product_variant_attributes (
    product_variant_id UUID NOT NULL REFERENCES product_variants (id) ON DELETE CASCADE,
    attribute_definition_id UUID NOT NULL REFERENCES attribute_definitions (id) ON DELETE RESTRICT,
    normalized_value VARCHAR(300) NOT NULL,
    display_value VARCHAR(300) NOT NULL,
    numeric_value NUMERIC(19, 4),
    boolean_value BOOLEAN,
    PRIMARY KEY (product_variant_id, attribute_definition_id)
);

CREATE INDEX idx_variant_attributes_filter_text
    ON product_variant_attributes (attribute_definition_id, normalized_value, product_variant_id);
CREATE INDEX idx_variant_attributes_filter_number
    ON product_variant_attributes (attribute_definition_id, numeric_value, product_variant_id)
    WHERE numeric_value IS NOT NULL;

CREATE TABLE collection_runs (
    id UUID PRIMARY KEY,
    market_id UUID NOT NULL REFERENCES chain_city_markets (id) ON DELETE CASCADE,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    status VARCHAR(30) NOT NULL,
    found_count INTEGER NOT NULL DEFAULT 0,
    changed_count INTEGER NOT NULL DEFAULT 0,
    error_count INTEGER NOT NULL DEFAULT 0,
    error_summary VARCHAR(2000),
    CONSTRAINT ck_collection_runs_status
        CHECK (status IN ('RUNNING', 'SUCCEEDED', 'PARTIALLY_SUCCEEDED', 'FAILED')),
    CONSTRAINT ck_collection_runs_counts
        CHECK (found_count >= 0 AND changed_count >= 0 AND error_count >= 0)
);

CREATE INDEX idx_collection_runs_market_started
    ON collection_runs (market_id, started_at DESC);
CREATE INDEX idx_collection_runs_failed
    ON collection_runs (started_at DESC, market_id)
    WHERE status IN ('PARTIALLY_SUCCEEDED', 'FAILED');

CREATE TABLE offers (
    id UUID PRIMARY KEY,
    market_id UUID NOT NULL REFERENCES chain_city_markets (id) ON DELETE CASCADE,
    product_variant_id UUID NOT NULL REFERENCES product_variants (id) ON DELETE RESTRICT,
    external_offer_id VARCHAR(300),
    offer_key VARCHAR(500) NOT NULL,
    condition_type VARCHAR(30) NOT NULL,
    condition_grade VARCHAR(50),
    battery_health_percent SMALLINT,
    completeness VARCHAR(500),
    warranty_months SMALLINT,
    regular_price NUMERIC(19, 2),
    sale_price NUMERIC(19, 2),
    conditional_price NUMERIC(19, 2),
    currency VARCHAR(3) NOT NULL,
    availability_status VARCHAR(30) NOT NULL,
    quantity INTEGER,
    product_url VARCHAR(2000) NOT NULL,
    promotion_valid_until TIMESTAMPTZ,
    last_checked_at TIMESTAMPTZ NOT NULL,
    last_seen_at TIMESTAMPTZ,
    data_fresh_until TIMESTAMPTZ,
    state_hash VARCHAR(64) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_offers_market_key UNIQUE (market_id, offer_key),
    CONSTRAINT uq_offers_id_market UNIQUE (id, market_id),
    CONSTRAINT ck_offers_condition_type
        CHECK (condition_type IN ('NEW', 'USED', 'REFURBISHED', 'DISPLAY')),
    CONSTRAINT ck_offers_availability
        CHECK (availability_status IN ('IN_STOCK', 'OUT_OF_STOCK', 'PREORDER', 'UNKNOWN')),
    CONSTRAINT ck_offers_prices_nonnegative CHECK (
        (regular_price IS NULL OR regular_price >= 0)
        AND (sale_price IS NULL OR sale_price >= 0)
        AND (conditional_price IS NULL OR conditional_price >= 0)
    ),
    CONSTRAINT ck_offers_has_price_or_unavailable CHECK (
        regular_price IS NOT NULL
        OR sale_price IS NOT NULL
        OR conditional_price IS NOT NULL
        OR availability_status IN ('OUT_OF_STOCK', 'UNKNOWN')
    ),
    CONSTRAINT ck_offers_quantity CHECK (quantity IS NULL OR quantity >= 0),
    CONSTRAINT ck_offers_battery_health CHECK (
        battery_health_percent IS NULL OR battery_health_percent BETWEEN 0 AND 100
    ),
    CONSTRAINT ck_offers_warranty CHECK (warranty_months IS NULL OR warranty_months >= 0)
);

CREATE UNIQUE INDEX uq_offers_market_external_id
    ON offers (market_id, external_offer_id)
    WHERE external_offer_id IS NOT NULL;
CREATE INDEX idx_offers_variant_market_current
    ON offers (product_variant_id, market_id, condition_type, id)
    INCLUDE (regular_price, sale_price, conditional_price, currency, availability_status, product_url, last_checked_at)
    WHERE active;
CREATE INDEX idx_offers_market_parser_lookup
    ON offers (market_id, external_offer_id, offer_key)
    INCLUDE (id, state_hash, last_checked_at, last_seen_at)
    WHERE active;
CREATE INDEX idx_offers_available_effective_price
    ON offers (product_variant_id, (COALESCE(sale_price, regular_price)), market_id)
    WHERE active AND availability_status IN ('IN_STOCK', 'PREORDER');
CREATE INDEX idx_offers_stale
    ON offers (data_fresh_until, market_id)
    WHERE active;

CREATE TABLE offer_terms (
    id UUID PRIMARY KEY,
    offer_id UUID NOT NULL REFERENCES offers (id) ON DELETE CASCADE,
    term_type VARCHAR(30) NOT NULL,
    title VARCHAR(300) NOT NULL,
    description VARCHAR(2000),
    monetary_value NUMERIC(19, 2),
    annual_rate NUMERIC(8, 4),
    term_months SMALLINT,
    monthly_payment NUMERIC(19, 2),
    initial_payment NUMERIC(19, 2),
    valid_from TIMESTAMPTZ,
    valid_until TIMESTAMPTZ,
    terms_url VARCHAR(2000),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_offer_terms_type CHECK (
        term_type IN ('GIFT', 'DISCOUNT', 'CONDITIONAL_PRICE', 'CASHBACK', 'TRADE_IN',
                      'INSTALLMENT', 'CREDIT', 'DELIVERY', 'OTHER')
    ),
    CONSTRAINT ck_offer_terms_money CHECK (
        (monetary_value IS NULL OR monetary_value >= 0)
        AND (monthly_payment IS NULL OR monthly_payment >= 0)
        AND (initial_payment IS NULL OR initial_payment >= 0)
        AND (annual_rate IS NULL OR annual_rate >= 0)
        AND (term_months IS NULL OR term_months > 0)
    ),
    CONSTRAINT ck_offer_terms_period CHECK (
        valid_from IS NULL OR valid_until IS NULL OR valid_from <= valid_until
    )
);

CREATE INDEX idx_offer_terms_offer_active_type
    ON offer_terms (offer_id, term_type, valid_until)
    WHERE active;
CREATE INDEX idx_offer_terms_metadata_gin ON offer_terms USING GIN (metadata);

CREATE TABLE store_inventories (
    offer_id UUID NOT NULL,
    store_location_id UUID NOT NULL,
    market_id UUID NOT NULL,
    availability_status VARCHAR(30) NOT NULL,
    quantity INTEGER,
    last_checked_at TIMESTAMPTZ NOT NULL,
    state_hash VARCHAR(64) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (offer_id, store_location_id),
    CONSTRAINT fk_store_inventories_offer_market
        FOREIGN KEY (offer_id, market_id) REFERENCES offers (id, market_id) ON DELETE CASCADE,
    CONSTRAINT fk_store_inventories_location_market
        FOREIGN KEY (store_location_id, market_id) REFERENCES store_locations (id, market_id) ON DELETE CASCADE,
    CONSTRAINT ck_store_inventories_availability
        CHECK (availability_status IN ('IN_STOCK', 'OUT_OF_STOCK', 'PREORDER', 'UNKNOWN')),
    CONSTRAINT ck_store_inventories_quantity CHECK (quantity IS NULL OR quantity >= 0)
);

CREATE INDEX idx_store_inventories_location_filter
    ON store_inventories (store_location_id, availability_status, offer_id)
    INCLUDE (quantity, last_checked_at);
CREATE INDEX idx_store_inventories_offer_filter
    ON store_inventories (offer_id, availability_status, store_location_id)
    INCLUDE (quantity, last_checked_at);

CREATE TABLE offer_state_history (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    offer_id UUID NOT NULL REFERENCES offers (id) ON DELETE CASCADE,
    source_run_id UUID REFERENCES collection_runs (id) ON DELETE SET NULL,
    regular_price NUMERIC(19, 2),
    sale_price NUMERIC(19, 2),
    conditional_price NUMERIC(19, 2),
    currency VARCHAR(3) NOT NULL,
    availability_status VARCHAR(30) NOT NULL,
    quantity INTEGER,
    terms_snapshot JSONB NOT NULL DEFAULT '[]'::jsonb,
    state_hash VARCHAR(64) NOT NULL,
    valid_from TIMESTAMPTZ NOT NULL,
    valid_to TIMESTAMPTZ,
    CONSTRAINT ck_offer_history_availability
        CHECK (availability_status IN ('IN_STOCK', 'OUT_OF_STOCK', 'PREORDER', 'UNKNOWN')),
    CONSTRAINT ck_offer_history_prices CHECK (
        (regular_price IS NULL OR regular_price >= 0)
        AND (sale_price IS NULL OR sale_price >= 0)
        AND (conditional_price IS NULL OR conditional_price >= 0)
    ),
    CONSTRAINT ck_offer_history_quantity CHECK (quantity IS NULL OR quantity >= 0),
    CONSTRAINT ck_offer_history_period CHECK (valid_to IS NULL OR valid_from < valid_to)
);

CREATE UNIQUE INDEX uq_offer_history_open_state
    ON offer_state_history (offer_id)
    WHERE valid_to IS NULL;
CREATE INDEX idx_offer_history_offer_range
    ON offer_state_history (offer_id, valid_from DESC)
    INCLUDE (valid_to, regular_price, sale_price, conditional_price, availability_status, quantity);
CREATE INDEX idx_offer_history_retention
    ON offer_state_history (valid_from, offer_id);
CREATE INDEX idx_offer_history_valid_from_brin
    ON offer_state_history USING BRIN (valid_from);

CREATE TABLE store_inventory_history (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    offer_id UUID NOT NULL REFERENCES offers (id) ON DELETE CASCADE,
    store_location_id UUID NOT NULL REFERENCES store_locations (id) ON DELETE CASCADE,
    source_run_id UUID REFERENCES collection_runs (id) ON DELETE SET NULL,
    availability_status VARCHAR(30) NOT NULL,
    quantity INTEGER,
    state_hash VARCHAR(64) NOT NULL,
    valid_from TIMESTAMPTZ NOT NULL,
    valid_to TIMESTAMPTZ,
    CONSTRAINT ck_inventory_history_availability
        CHECK (availability_status IN ('IN_STOCK', 'OUT_OF_STOCK', 'PREORDER', 'UNKNOWN')),
    CONSTRAINT ck_inventory_history_quantity CHECK (quantity IS NULL OR quantity >= 0),
    CONSTRAINT ck_inventory_history_period CHECK (valid_to IS NULL OR valid_from < valid_to)
);

CREATE UNIQUE INDEX uq_inventory_history_open_state
    ON store_inventory_history (offer_id, store_location_id)
    WHERE valid_to IS NULL;
CREATE INDEX idx_inventory_history_store_offer_range
    ON store_inventory_history (store_location_id, offer_id, valid_from DESC)
    INCLUDE (valid_to, availability_status, quantity);
CREATE INDEX idx_inventory_history_retention
    ON store_inventory_history (valid_from, offer_id, store_location_id);
CREATE INDEX idx_inventory_history_valid_from_brin
    ON store_inventory_history USING BRIN (valid_from);

CREATE TABLE shipping_quotes (
    id UUID PRIMARY KEY,
    offer_id UUID NOT NULL REFERENCES offers (id) ON DELETE CASCADE,
    destination_city_id UUID NOT NULL REFERENCES cities (id) ON DELETE RESTRICT,
    available BOOLEAN NOT NULL,
    delivery_price NUMERIC(19, 2),
    currency VARCHAR(3) NOT NULL,
    min_delivery_days SMALLINT,
    max_delivery_days SMALLINT,
    last_checked_at TIMESTAMPTZ NOT NULL,
    state_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_shipping_quotes_offer_destination UNIQUE (offer_id, destination_city_id),
    CONSTRAINT ck_shipping_quotes_price CHECK (delivery_price IS NULL OR delivery_price >= 0),
    CONSTRAINT ck_shipping_quotes_days CHECK (
        (min_delivery_days IS NULL OR min_delivery_days >= 0)
        AND (max_delivery_days IS NULL OR max_delivery_days >= 0)
        AND (min_delivery_days IS NULL OR max_delivery_days IS NULL OR min_delivery_days <= max_delivery_days)
    )
);

CREATE INDEX idx_shipping_quotes_destination_offer
    ON shipping_quotes (destination_city_id, offer_id)
    INCLUDE (available, delivery_price, min_delivery_days, max_delivery_days, last_checked_at)
    WHERE available;

CREATE TABLE offer_monthly_stats (
    offer_id UUID NOT NULL REFERENCES offers (id) ON DELETE CASCADE,
    month DATE NOT NULL,
    currency VARCHAR(3) NOT NULL,
    average_effective_price NUMERIC(19, 2),
    minimum_effective_price NUMERIC(19, 2),
    maximum_effective_price NUMERIC(19, 2),
    first_effective_price NUMERIC(19, 2),
    last_effective_price NUMERIC(19, 2),
    in_stock_days SMALLINT NOT NULL DEFAULT 0,
    out_of_stock_days SMALLINT NOT NULL DEFAULT 0,
    unknown_days SMALLINT NOT NULL DEFAULT 0,
    aggregated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (offer_id, month),
    CONSTRAINT ck_offer_monthly_stats_month CHECK (month = DATE_TRUNC('month', month)::date),
    CONSTRAINT ck_offer_monthly_stats_days CHECK (
        in_stock_days >= 0 AND out_of_stock_days >= 0 AND unknown_days >= 0
    )
);

CREATE INDEX idx_offer_monthly_stats_offer_month
    ON offer_monthly_stats (offer_id, month DESC);

CREATE TABLE store_inventory_monthly_stats (
    offer_id UUID NOT NULL REFERENCES offers (id) ON DELETE CASCADE,
    store_location_id UUID NOT NULL REFERENCES store_locations (id) ON DELETE CASCADE,
    month DATE NOT NULL,
    in_stock_days SMALLINT NOT NULL DEFAULT 0,
    out_of_stock_days SMALLINT NOT NULL DEFAULT 0,
    unknown_days SMALLINT NOT NULL DEFAULT 0,
    aggregated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (offer_id, store_location_id, month),
    CONSTRAINT ck_inventory_monthly_stats_month CHECK (month = DATE_TRUNC('month', month)::date),
    CONSTRAINT ck_inventory_monthly_stats_days CHECK (
        in_stock_days >= 0 AND out_of_stock_days >= 0 AND unknown_days >= 0
    )
);

CREATE INDEX idx_inventory_monthly_store_offer
    ON store_inventory_monthly_stats (store_location_id, offer_id, month DESC);

INSERT INTO retail_chains (id, name, code, website_url, parser_type, parser_enabled, created_at, updated_at)
SELECT gen_random_uuid(), grouped.name, grouped.code, grouped.website_url,
       grouped.parser_type, grouped.parser_enabled, grouped.created_at, NOW()
FROM (
    SELECT DISTINCT ON (LOWER(BTRIM(name)))
           name,
           'LEGACY-' || SUBSTRING(MD5(LOWER(BTRIM(name))) FROM 1 FOR 12) AS code,
           website_url,
           parser_type,
           parser_enabled,
           created_at
    FROM legacy_stores
    ORDER BY LOWER(BTRIM(name)), created_at, id
) grouped;

INSERT INTO chain_city_markets (
    id, retail_chain_id, city_id, sales_channel, external_market_id,
    source_base_url, parser_enabled, created_at, updated_at
)
SELECT gen_random_uuid(), rc.id, ls.city_id, 'PHYSICAL',
       'legacy:' || ls.city_id, rc.website_url, rc.parser_enabled,
       MIN(ls.created_at), NOW()
FROM legacy_stores ls
JOIN retail_chains rc ON LOWER(rc.name) = LOWER(BTRIM(ls.name))
GROUP BY rc.id, ls.city_id, rc.website_url, rc.parser_enabled;

INSERT INTO store_locations (
    id, retail_chain_id, market_id, external_store_id, name, website_url,
    active, created_at, updated_at
)
SELECT ls.id, rc.id, market.id, 'legacy:' || ls.id, ls.name, ls.website_url,
       TRUE, ls.created_at, NOW()
FROM legacy_stores ls
JOIN retail_chains rc ON LOWER(rc.name) = LOWER(BTRIM(ls.name))
JOIN chain_city_markets market
  ON market.retail_chain_id = rc.id
 AND market.city_id = ls.city_id
 AND market.sales_channel = 'PHYSICAL';

INSERT INTO product_models (
    id, name, catalog_key, description, created_at, updated_at
)
SELECT id, name, sku, description, created_at, NOW()
FROM legacy_products;

INSERT INTO product_variants (
    id, product_model_id, canonical_key, manufacturer_sku, display_name, created_at, updated_at
)
SELECT gen_random_uuid(), id, 'default', sku, name, created_at, NOW()
FROM legacy_products;

WITH latest_prices AS (
    SELECT DISTINCT ON (market.id, variant.id)
           market.id AS market_id,
           variant.id AS variant_id,
           lp.product_id,
           lp.amount,
           lp.currency,
           lp.product_url,
           lp.observed_at
    FROM legacy_prices lp
    JOIN store_locations location ON location.id = lp.store_id
    JOIN chain_city_markets market ON market.id = location.market_id
    JOIN product_variants variant ON variant.product_model_id = lp.product_id
    ORDER BY market.id, variant.id, lp.observed_at DESC, lp.id DESC
)
INSERT INTO offers (
    id, market_id, product_variant_id, external_offer_id, offer_key,
    condition_type, regular_price, currency, availability_status,
    product_url, last_checked_at, last_seen_at, state_hash,
    active, created_at, updated_at
)
SELECT gen_random_uuid(), market_id, variant_id,
       'legacy:' || product_id, 'legacy:' || product_id,
       'NEW', amount, currency, 'IN_STOCK', product_url,
       observed_at, observed_at,
       MD5(amount::text || '|' || currency || '|IN_STOCK'),
       TRUE, observed_at, NOW()
FROM latest_prices;

WITH raw_history AS (
    SELECT offer.id AS offer_id,
           lp.id AS legacy_price_id,
           lp.amount,
           lp.currency,
           lp.observed_at
    FROM legacy_prices lp
    JOIN store_locations location ON location.id = lp.store_id
    JOIN product_variants variant ON variant.product_model_id = lp.product_id
    JOIN offers offer
      ON offer.market_id = location.market_id
     AND offer.product_variant_id = variant.id
     AND offer.offer_key = 'legacy:' || lp.product_id
), deduplicated_history AS (
    SELECT DISTINCT ON (offer_id, observed_at)
           offer_id, amount, currency, observed_at
    FROM raw_history
    ORDER BY offer_id, observed_at, legacy_price_id DESC
), ordered_history AS (
    SELECT offer_id,
           amount,
           currency,
           observed_at,
           LEAD(observed_at) OVER (
               PARTITION BY offer_id ORDER BY observed_at
           ) AS valid_to
    FROM deduplicated_history
)
INSERT INTO offer_state_history (
    offer_id, regular_price, currency, availability_status,
    state_hash, valid_from, valid_to
)
SELECT offer_id, amount, currency, 'IN_STOCK',
       MD5(amount::text || '|' || currency || '|IN_STOCK'),
       observed_at, valid_to
FROM ordered_history;

COMMENT ON TABLE retail_chains IS 'Retail brands/chains; parser configuration belongs here rather than to each location.';
COMMENT ON TABLE chain_city_markets IS 'One pricing scope for a retail chain, city and sales channel.';
COMMENT ON TABLE product_models IS 'Canonical product model shown once in the catalog.';
COMMENT ON TABLE product_variants IS 'A sellable manufacturer configuration such as storage and color.';
COMMENT ON TABLE offers IS 'Current city-level offer state; physical locations in the same market share this price.';
COMMENT ON TABLE offer_state_history IS 'Change-only offer state intervals used to reconstruct daily price charts.';
COMMENT ON TABLE collection_runs IS 'Daily parser run log; failures must not be interpreted as out-of-stock.';
