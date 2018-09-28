CREATE TABLE app_user (
    id SERIAL PRIMARY KEY,
    email VARCHAR UNIQUE NOT NULL,
    password VARCHAR(60),
    first_name VARCHAR(30),
    last_name VARCHAR(30),
    country_id INT REFERENCES country(id),
    phone_number VARCHAR,
    role_id INT REFERENCES role(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    auth_method VARCHAR(8) NOT NULL,
    enabled BOOLEAN NOT NULL
);