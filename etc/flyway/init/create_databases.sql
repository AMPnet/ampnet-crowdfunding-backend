DROP DATABASE IF EXISTS crowdfunding;
CREATE DATABASE crowdfunding ENCODING 'UTF-8';

DROP DATABASE IF EXISTS crowdfunding_test;
CREATE DATABASE crowdfunding_test ENCODING 'UTF-8';

DROP USER IF EXISTS crowdfunding;
CREATE USER crowdfunding WITH PASSWORD 'password';

DROP USER IF EXISTS crowdfunding_test;
CREATE USER crowdfunding_test WITH PASSWORD 'password';

DROP ROLE IF EXISTS cf_role;
CREATE ROLE cf_role;
GRANT cf_role TO crowdfunding;
GRANT cf_role TO crowdfunding_test;
GRANT ALL PRIVILEGES ON DATABASE crowdfunding_test TO cf_role;
