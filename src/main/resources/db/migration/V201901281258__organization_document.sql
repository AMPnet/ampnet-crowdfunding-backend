ALTER TABLE organization DROP COLUMN documents;

CREATE TABLE organization_document(
    organization_id INT REFERENCES organization(id) NOT NULL,
    document_id INT REFERENCES document(id) NOT NULL,

    PRIMARY KEY (organization_id, document_id)
);
