-- 1. product_id darf NULL sein (für Füllartikel / Beikäufe)
ALTER TABLE order_items
    ALTER COLUMN product_id DROP NOT NULL;

-- 2. Inventar-, Resale- und Snapshot-Felder hinzufügen
ALTER TABLE order_items
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'IN_STOCK' CHECK (status IN ('IN_STOCK', 'SOLD', 'KEPT')),
    ADD COLUMN resale_price NUMERIC(10, 2),
    ADD COLUMN resale_platform VARCHAR(50),
    ADD COLUMN resold_at TIMESTAMPTZ,
    ADD COLUMN market_price_snapshot NUMERIC(10, 2),
    ADD COLUMN is_filler BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN custom_name TEXT,
    ADD COLUMN filler_cost NUMERIC(10, 2);

-- 3. Performance-Index für Inventar-Abfragen
CREATE INDEX idx_order_items_status ON order_items(status);