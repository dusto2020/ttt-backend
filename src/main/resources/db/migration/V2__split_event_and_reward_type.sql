ALTER TABLE orders
    ADD COLUMN reward_type VARCHAR(30) NOT NULL DEFAULT 'temu_credit'
        CHECK (reward_type IN ('temu_credit', 'paypal_cashback'));

UPDATE orders
    SET reward_type = 'paypal_cashback'
    WHERE event_type = 'paypal_cashback';

UPDATE orders
    SET event_type = 'claimcredit'
    WHERE event_type = 'paypal_cashback';

ALTER TABLE orders
    DROP CONSTRAINT IF EXISTS orders_event_type_check;

ALTER TABLE orders
    ADD CONSTRAINT orders_event_type_check CHECK (event_type IN ('claimcredit', 'wincredit'));
