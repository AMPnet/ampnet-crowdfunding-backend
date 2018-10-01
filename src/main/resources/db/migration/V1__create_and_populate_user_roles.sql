CREATE TABLE role (
  id INT PRIMARY KEY,
  name VARCHAR NOT NULL,
  description VARCHAR NOT NULL
);

INSERT INTO role VALUES
  (1, 'ADMIN', 'Administrators can create new projects to be funded and manage other platform components.');
INSERT INTO role VALUES
  (2, 'USER', 'Regular users invest in offered projects, track their portfolio and manage funds on their wallet.');