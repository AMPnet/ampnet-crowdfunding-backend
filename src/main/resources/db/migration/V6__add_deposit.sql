CREATE TABLE deposit(
    id SERIAL PRIMARY KEY,
    user_uuid UUID NOT NULL,
    reference VARCHAR(16) NOT NULL,
    approved BOOLEAN NOT NULL,
    approved_by_user_uuid UUID,
    approved_at TIMESTAMP,
    amount BIGINT,
    document_id INT REFERENCES document(id),
    created_at TIMESTAMP NOT NULL
);
