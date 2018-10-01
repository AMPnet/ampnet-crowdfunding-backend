CREATE TABLE country (
  id SERIAL PRIMARY KEY,
  iso VARCHAR(2) NOT NULL,
  name VARCHAR(80) NOT NULL,
  nicename VARCHAR(80) NOT NULL,
  iso3 VARCHAR(3) DEFAULT NULL,
  numcode SMALLINT DEFAULT NULL,
  phonecode INT NOT NULL
);

-- few countries for testing purposes, full list available in ../countries/complete_countries_list.sql
INSERT INTO country VALUES
(1, 'BA', 'BOSNIA AND HERZEGOVINA', 'Bosnia and Herzegovina', 'BIH', 70, 387),
(2, 'BG', 'BULGARIA', 'Bulgaria', 'BGR', 100, 359),
(3, 'HR', 'CROATIA', 'Croatia', 'HRV', 191, 385),
(4, 'EE', 'ESTONIA', 'Estonia', 'EST', 233, 372),
(5, 'LI', 'LIECHTENSTEIN', 'Liechtenstein', 'LIE', 438, 423);

