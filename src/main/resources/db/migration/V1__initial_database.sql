-- Role
CREATE TABLE role (
  id INT PRIMARY KEY,
  name VARCHAR NOT NULL,
  description VARCHAR NOT NULL
);
INSERT INTO role VALUES
  (1, 'ORG_ADMIN', 'Administrators can manage users in organization.');
INSERT INTO role VALUES
  (2, 'ORG_MEMBER', 'Members can use organization.');

-- Wallet
CREATE TABLE wallet (
    id SERIAL PRIMARY KEY,
    hash VARCHAR(66) UNIQUE NOT NULL,
    type VARCHAR(8) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE TABLE user_wallet (
    id SERIAL PRIMARY KEY,
    user_uuid UUID NOT NULL,
    wallet_id INT REFERENCES wallet(id) NOT NULL
);
CREATE TABLE pair_wallet_code(
    id SERIAL PRIMARY KEY,
    address VARCHAR(66) UNIQUE NOT NULL,
    public_key VARCHAR UNIQUE NOT NULL,
    code VARCHAR(6) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL
);

-- Organization
CREATE TABLE organization (
    id SERIAL PRIMARY KEY,
    name VARCHAR NOT NULL UNIQUE,
    created_by_user_uuid UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    approved BOOLEAN NOT NULL,
    approved_by_user_uuid UUID,
    legal_info VARCHAR,
    wallet_id INT REFERENCES wallet(id)
);
CREATE TABLE organization_membership (
    id SERIAL PRIMARY KEY,
    organization_id INT REFERENCES organization(id) NOT NULL,
    user_uuid UUID NOT NULL,
    role_id INT REFERENCES role(id),
    created_at TIMESTAMP NOT NULL
);
CREATE TABLE organization_follower (
    id SERIAL PRIMARY KEY,
    organization_id INT REFERENCES organization(id) NOT NULL,
    user_uuid UUID NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE TABLE organization_invitation (
    id SERIAL PRIMARY KEY,
    email VARCHAR NOT NULL,
    organization_id INT REFERENCES organization(id) NOT NULL,
    invited_by_user_uuid UUID NOT NULL,
    role_id INT REFERENCES role(id),
    created_at TIMESTAMP NOT NULL
);

-- Project
CREATE TABLE project (
    id SERIAL PRIMARY KEY,
    organization_id INT REFERENCES organization(id) NOT NULL,
    name VARCHAR NOT NULL,
    description TEXT NOT NULL,
    location VARCHAR(128) NOT NULL,
    location_text VARCHAR NOT NULL,
    return_on_investment VARCHAR(16) NOT NULL,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    expected_funding BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    min_per_user BIGINT NOT NULL,
    max_per_user BIGINT NOT NULL,
    main_image VARCHAR,
    gallery TEXT,
    news_links TEXT,
    created_by_user_uuid UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    wallet_id INT REFERENCES wallet(id),
    active BOOLEAN NOT NULL
);

-- Document
CREATE TABLE document (
    id SERIAL PRIMARY KEY,
    link VARCHAR NOT NULL,
    name VARCHAR NOT NULL,
    type VARCHAR(16) NOT NULL,
    size INT NOT NULL,
    created_by_user_uuid UUID NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE TABLE project_document(
    project_id INT REFERENCES project(id) NOT NULL,
    document_id INT REFERENCES document(id) NOT NULL,

    PRIMARY KEY (project_id, document_id)
);
CREATE TABLE organization_document(
    organization_id INT REFERENCES organization(id) NOT NULL,
    document_id INT REFERENCES document(id) NOT NULL,

    PRIMARY KEY (organization_id, document_id)
);

-- Transaction
CREATE TABLE transaction_info (
  id SERIAL PRIMARY KEY,
  type VARCHAR(16) NOT NULL,
  title VARCHAR NOT NULL,
  description VARCHAR NOT NULL,
  user_uuid UUID NOT NULL,
  companion_id INT
);

-- Deposit
CREATE TABLE deposit(
    id SERIAL PRIMARY KEY,
    user_uuid UUID NOT NULL,
    reference VARCHAR(16) NOT NULL,
    amount BIGINT NOT NULL,
    approved BOOLEAN NOT NULL,
    approved_by_user_uuid UUID,
    approved_at TIMESTAMP,
    document_id INT REFERENCES document(id),
    tx_hash VARCHAR,
    created_at TIMESTAMP NOT NULL
);

-- Withdraw
CREATE TABLE withdraw(
    id SERIAL PRIMARY KEY,
    user_uuid UUID NOT NULL,
    amount BIGINT NOT NULL,
    bank_account VARCHAR(64) NOT NULL,
    approved_tx_hash VARCHAR,
    approved_at TIMESTAMP,
    burned_tx_hash VARCHAR,
    burned_at TIMESTAMP,
    burned_by UUID,
    document_id INT REFERENCES document(id),
    created_at TIMESTAMP NOT NULL
);
