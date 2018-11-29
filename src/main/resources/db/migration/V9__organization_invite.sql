CREATE TABLE organization_invite (
    user_id INT REFERENCES app_user(id) NOT NULL,
    organization_id INT REFERENCES organization(id) NOT NULL,
    invited_by INT REFERENCES app_user(id) NOT NULL,
    role_id INT REFERENCES role(id),
    created_at TIMESTAMP NOT NULL,

    PRIMARY KEY (organization_id, user_id)
);
