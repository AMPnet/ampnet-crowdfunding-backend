CREATE TABLE organization_membership (
    id SERIAL PRIMARY KEY,
    organization_id INT REFERENCES organization(id) NOT NULL,
    user_id INT REFERENCES app_user(id) NOT NULL,
    role_id INT REFERENCES role(id),
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT organization_membership_unique UNIQUE (organization_id, user_id)
);

CREATE INDEX idx_org_memb_org_id ON organization_membership(organization_id);
CREATE INDEX idx_org_memb_user_id ON organization_membership(user_id);

INSERT INTO role VALUES
  (3, 'ORG_ADMIN', 'Administrators can manage users in organization.');
INSERT INTO role VALUES
  (4, 'ORG_MEMBER', 'Members can use organization.');
