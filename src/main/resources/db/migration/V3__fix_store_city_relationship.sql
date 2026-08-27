DROP TABLE store_cities;

ALTER TABLE stores
    ADD COLUMN city_id UUID REFERENCES cities(id);

ALTER TABLE stores
    DROP CONSTRAINT stores_name_key;
