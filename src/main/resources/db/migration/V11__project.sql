CREATE TABLE document (
    id SERIAL PRIMARY KEY,
    hash VARCHAR NOT NULL,
    name VARCHAR NOT NULL,
    type VARCHAR(16) NOT NULL,
    size INT NOT NULL,
    created_by INT REFERENCES app_user(id) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_document_hash ON document(hash);

CREATE TABLE project (
    id SERIAL PRIMARY KEY,
    organization_id INT REFERENCES organization(id) NOT NULL,
    name VARCHAR NOT NULL,
    description TEXT NOT NULL,
    location VARCHAR(128) NOT NULL,
    location_text VARCHAR NOT NULL,
    return_to_investment VARCHAR(16) NOT NULL,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    expected_funding DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    min_per_user DECIMAL(10,2) NOT NULL,
    max_per_user DECIMAL(10,2) NOT NULL,
    main_image VARCHAR,
    gallery TEXT,
    created_by INT REFERENCES app_user(id) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    wallet_id INT REFERENCES wallet(id),
    active BOOLEAN
);

CREATE TABLE project_document(
    project_id INT REFERENCES project(id) NOT NULL,
    document_id INT REFERENCES document(id) NOT NULL,

    PRIMARY KEY (project_id, document_id)
);
