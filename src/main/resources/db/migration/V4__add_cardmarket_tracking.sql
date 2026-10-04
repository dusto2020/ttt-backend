ALTER TABLE products
    ADD COLUMN cardmarket_lowest_price NUMERIC(10, 2),
    ADD COLUMN cardmarket_updated_at TIMESTAMPTZ,
    ADD COLUMN manual_price_override NUMERIC(10, 2);
