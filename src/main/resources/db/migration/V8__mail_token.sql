CREATE TABLE mail_token (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES app_user(id) NOT NULL,
    token UUID NOT NULL,
    created_at TIMESTAMP NOT NULL
);
