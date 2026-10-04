CREATE TABLE whitelisted_users (
                                   discord_id VARCHAR(50) PRIMARY KEY,
                                   discord_username VARCHAR(100),
                                   notes TEXT,
                                   created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Füge deine eigene Discord-ID direkt als Initialwert ein (z. B. '1234567890')
INSERT INTO whitelisted_users (discord_id, discord_username, notes) VALUES ('487564345639698433', 'dusto2020', 'Owner');