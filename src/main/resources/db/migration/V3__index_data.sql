CREATE INDEX idx_app_user_email ON app_user(email);
CREATE INDEX idx_org_follower_org_id ON organization_membership(organization_id);
CREATE INDEX idx_document_hash ON document(hash);
CREATE INDEX idx_mail_token_token ON mail_token(token);

CREATE INDEX idx_organization_name ON organization(name);
CREATE INDEX idx_project_name ON project(name);