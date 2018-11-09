CREATE TABLE organization (
    id SERIAL PRIMARY KEY,
    name VARCHAR NOT NULL UNIQUE,
    created_by INT REFERENCES app_user(id) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    approved BOOLEAN NOT NULL,
    legal_info VARCHAR,    -- TODO: change legal info and set NOT NULL
    documents TEXT
);
