-- Role
CREATE TABLE role (
  id INT PRIMARY KEY,
  name VARCHAR NOT NULL,
  description VARCHAR NOT NULL
);

-- Wallet
CREATE TABLE wallet (
    id SERIAL PRIMARY KEY,
    hash VARCHAR(66) UNIQUE NOT NULL,
    type VARCHAR(8) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

-- User
CREATE TABLE app_user (
    id SERIAL PRIMARY KEY,
    email VARCHAR UNIQUE NOT NULL,
    password VARCHAR(60),
    first_name VARCHAR(30),
    last_name VARCHAR(30),
    phone_number VARCHAR,
    role_id INT REFERENCES role(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    auth_method VARCHAR(8) NOT NULL,
    enabled BOOLEAN NOT NULL,
    wallet_id INT REFERENCES wallet(id)
);

-- Organization
CREATE TABLE organization (
    id SERIAL PRIMARY KEY,
    name VARCHAR NOT NULL UNIQUE,
    created_by_user_uuid VARCHAR NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    approved BOOLEAN NOT NULL,
    approved_by_user_uuid VARCHAR,
    legal_info VARCHAR,
    wallet_id INT REFERENCES wallet(id)
);
CREATE TABLE organization_membership (
    id SERIAL PRIMARY KEY,
    organization_id INT REFERENCES organization(id) NOT NULL,
    user_uuid VARCHAR NOT NULL,
    role_id INT REFERENCES role(id),
    created_at TIMESTAMP NOT NULL
);
CREATE TABLE organization_follower (
    id SERIAL PRIMARY KEY,
    organization_id INT REFERENCES organization(id) NOT NULL,
    user_uuid VARCHAR NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE TABLE organization_invite (
    id SERIAL PRIMARY KEY,
    email VARCHAR NOT NULL,
    organization_id INT REFERENCES organization(id) NOT NULL,
    invited_by_user_uuid VARCHAR NOT NULL,
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
    created_by INT REFERENCES app_user(id) NOT NULL,
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
    created_by_user_uuid VARCHAR NOT NULL,
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
  user_id INT REFERENCES app_user(id) NOT NULL,
  companion_id INT
);
