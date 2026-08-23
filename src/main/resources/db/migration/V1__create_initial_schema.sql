CREATE TABLE products (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    sku VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE stores (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE,
    website_url VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE prices (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores (id) ON DELETE CASCADE,
    amount NUMERIC(19, 2) NOT NULL CHECK (amount >= 0),
    currency VARCHAR(3) NOT NULL,
    product_url VARCHAR(2000) NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_prices_product_observed ON prices (product_id, observed_at DESC);
CREATE INDEX idx_prices_store ON prices (store_id);
