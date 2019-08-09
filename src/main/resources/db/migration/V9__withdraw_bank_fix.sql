ALTER TABLE withdraw DROP COLUMN bank_account_id;
ALTER TABLE withdraw ADD COLUMN bank_account VARCHAR(64) NOT NULL DEFAULT '';
