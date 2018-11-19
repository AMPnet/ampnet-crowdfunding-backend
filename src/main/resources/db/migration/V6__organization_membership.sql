CREATE TABLE organization_membership (
    organization_id INT REFERENCES organization(id) NOT NULL,
    user_id INT REFERENCES app_user(id) NOT NULL,
    role_id INT REFERENCES role(id),
    created_at TIMESTAMP NOT NULL,

    PRIMARY KEY (organization_id, user_id)
);

INSERT INTO role VALUES
  (3, 'ORG_ADMIN', 'Administrators can manage users in organization.');
INSERT INTO role VALUES
  (4, 'ORG_MEMBER', 'Members can use organization.');
