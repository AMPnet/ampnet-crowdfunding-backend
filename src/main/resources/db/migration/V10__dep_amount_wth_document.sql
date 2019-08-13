ALTER TABLE deposit ALTER COLUMN amount SET NOT NULL;
ALTER TABLE withdraw ADD COLUMN document_id INT REFERENCES document(id);
