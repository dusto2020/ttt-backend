ALTER TABLE sellers
    ADD COLUMN country_code VARCHAR(10) NOT NULL DEFAULT 'DE' CHECK (country_code IN ('DE', 'AT'));