INSERT INTO cities (id, name, created_at)
SELECT gen_random_uuid(), 'Не указан', NOW()
WHERE NOT EXISTS (SELECT 1 FROM cities)
  AND EXISTS (SELECT 1 FROM stores WHERE city_id IS NULL);

UPDATE stores
SET city_id = (SELECT id FROM cities ORDER BY created_at, id LIMIT 1)
WHERE city_id IS NULL
  AND EXISTS (SELECT 1 FROM cities);

ALTER TABLE stores
    ALTER COLUMN city_id SET NOT NULL;
