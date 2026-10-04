ALTER TABLE products
    ADD COLUMN country_code VARCHAR(10) NOT NULL DEFAULT 'DE' CHECK (country_code IN ('DE', 'AT'));

ALTER TABLE products
    ALTER COLUMN language_code SET DEFAULT 'DE';

ALTER TABLE products
    ADD CONSTRAINT products_language_code_check CHECK (language_code IN ('DE', 'EN', 'JP'));

ALTER TABLE orders
    ADD COLUMN country_code VARCHAR(10) NOT NULL DEFAULT 'DE' CHECK (country_code IN ('DE', 'AT'));
