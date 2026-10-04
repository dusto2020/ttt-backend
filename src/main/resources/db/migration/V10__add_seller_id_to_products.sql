-- 1. seller_id Spalte hinzufügen
ALTER TABLE products
    ADD COLUMN seller_id UUID REFERENCES sellers(id) ON DELETE SET NULL;

CREATE INDEX idx_products_seller_id ON products(seller_id);

-- 2. Automatische Daten-Migration für bestehende Produkte:
-- Verknüpft alle vorhandenen AT- und DE-Produkte direkt mit dem passenden Heartforcards-Eintrag!
UPDATE products p
SET seller_id = s.id
FROM sellers s
WHERE s.name ILIKE '%Heartforcards%'
  AND s.country_code = p.country_code;