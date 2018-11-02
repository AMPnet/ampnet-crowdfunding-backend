CREATE TABLE wallet (
    id SERIAL PRIMARY KEY,
    owner_id INT REFERENCES app_user(id) NOT NULL UNIQUE,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE transaction (
    id SERIAL PRIMARY KEY,
    wallet_id INT REFERENCES wallet(id) NOT NULL,
    sender VARCHAR(60) NOT NULL,
    receiver VARCHAR(60) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    type VARCHAR(8) NOT NULL,
    tx_hash VARCHAR(66) NOT NULL,
    timestamp TIMESTAMP  NOT NULL
);
