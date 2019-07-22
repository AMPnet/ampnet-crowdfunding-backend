CREATE TABLE pair_wallet_code(
    id SERIAL PRIMARY KEY,
    address VARCHAR(66) UNIQUE NOT NULL,
    public_key VARCHAR UNIQUE NOT NULL,
    code VARCHAR(6) UNIQUE NOT NULL
);
CREATE INDEX idx_pair_wallet_code_address ON pair_wallet_code(address);
