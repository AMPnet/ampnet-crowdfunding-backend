ALTER TABLE pair_wallet_code ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT(NOW());
