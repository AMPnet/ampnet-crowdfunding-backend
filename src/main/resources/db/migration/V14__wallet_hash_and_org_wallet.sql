ALTER TABLE wallet DROP COLUMN address;
ALTER TABLE wallet ADD COLUMN hash VARCHAR(66) UNIQUE NOT NULL;

CREATE TABLE wallet_token (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES app_user(id) NOT NULL,
    token UUID NOT NULL,
    created_at TIMESTAMP NOT NULL
);

ALTER TABLE organization ADD COLUMN wallet_id INT REFERENCES wallet(id);
