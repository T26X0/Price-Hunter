ALTER TABLE stores
    ADD COLUMN parser_type VARCHAR(100),
ADD COLUMN parser_enabled BOOLEAN NOT NULL DEFAULT TRUE;


CREATE TABLE cities (
                        id UUID PRIMARY KEY,
                        name VARCHAR(200) NOT NULL UNIQUE,
                        created_at TIMESTAMPTZ NOT NULL
);


CREATE TABLE store_cities (
                              store_id UUID NOT NULL REFERENCES stores (id) ON DELETE CASCADE,
                              city_id UUID NOT NULL REFERENCES cities (id) ON DELETE CASCADE,

                              PRIMARY KEY (store_id, city_id)
);