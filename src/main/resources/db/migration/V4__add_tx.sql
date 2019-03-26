CREATE TABLE transaction_info (
  id SERIAL PRIMARY KEY,
  type VARCHAR(16) NOT NULL,
  title VARCHAR NOT NULL,
  description VARCHAR NOT NULL,
  user_id INT REFERENCES app_user(id) NOT NULL,
  companion_id INT
);
