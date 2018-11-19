CREATE TABLE organization_follower (
    organization_id INT REFERENCES organization(id) NOT NULL,
    user_id INT REFERENCES app_user(id) NOT NULL,
    created_at TIMESTAMP NOT NULL,

    PRIMARY KEY (organization_id, user_id)
);

CREATE INDEX idx_org_follower_org_id ON organization_membership(organization_id);
