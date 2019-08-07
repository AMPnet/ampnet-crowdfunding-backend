DROP TABLE withdraw;
CREATE TABLE withdraw(
    id SERIAL PRIMARY KEY,
    user UUID NOT NULL,
    amount BIGINT NOT NULL,
    approved_tx_hash VARCHAR,
    approved_at TIMESTAMP,
    burned_tx_hash VARCHAR,
    burned_at TIMESTAMP,
    burned_by UUID,
    created_at TIMESTAMP NOT NULL
);
