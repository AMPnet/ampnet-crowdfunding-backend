INSERT INTO role VALUES
  (1, 'ADMIN', 'Administrators can create new projects to be funded and manage other platform components.');
INSERT INTO role VALUES
  (2, 'USER', 'Regular users invest in offered projects, track their portfolio and manage funds on their wallet.');
INSERT INTO role VALUES
  (3, 'ORG_ADMIN', 'Administrators can manage users in organization.');
INSERT INTO role VALUES
  (4, 'ORG_MEMBER', 'Members can use organization.');

-- few countries for testing purposes, full list available in ../countries/complete_countries_list.sql
INSERT INTO country VALUES
(1, 'BA', 'BOSNIA AND HERZEGOVINA', 'Bosnia and Herzegovina', 'BIH', 70, 387),
(2, 'BG', 'BULGARIA', 'Bulgaria', 'BGR', 100, 359),
(3, 'HR', 'CROATIA', 'Croatia', 'HRV', 191, 385),
(4, 'EE', 'ESTONIA', 'Estonia', 'EST', 233, 372),
(5, 'LI', 'LIECHTENSTEIN', 'Liechtenstein', 'LIE', 438, 423);

