CREATE INDEX idx_org_follower_org_id ON organization_membership(organization_id);
CREATE INDEX idx_document_hash ON document(link);

CREATE INDEX idx_organization_name ON organization(name);
CREATE INDEX idx_project_name ON project(name);

CREATE INDEX idx_transaction_info_user_uuid ON transaction_info(user_uuid);
CREATE INDEX idx_user_wallet_user_uuid ON user_wallet(user_uuid);
CREATE INDEX idx_organization_membership_user_uuid ON organization_membership(user_uuid);

CREATE INDEX idx_pair_wallet_code_address ON pair_wallet_code(address);

CREATE INDEX idx_deposit_reference ON deposit(reference);
CREATE INDEX idx_deposit_user_uuid ON deposit(user_uuid);
CREATE INDEX idx_withdraw_user_uuid ON withdraw(user_uuid);
