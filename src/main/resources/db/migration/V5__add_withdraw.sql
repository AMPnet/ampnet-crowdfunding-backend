CREATE TABLE withdraw(
    id SERIAL PRIMARY KEY,
    user_uuid UUID NOT NULL,
    amount BIGINT NOT NULL,
    approved BOOLEAN NOT NULL,
    approved_reference VARCHAR,
    approved_by_user_uuid UUID,
    approved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL
);
