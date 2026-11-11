-- =====================================================================
--  CampusRide - one-time setup, run ONCE as the MySQL root user.
--  Creates a separate login for the app so we never use root in Java.
--  Change the password here AND in src/main/resources/db.properties.
-- =====================================================================
CREATE USER IF NOT EXISTS 'campusride'@'localhost' IDENTIFIED BY 'CampusRide@123';
GRANT ALL PRIVILEGES ON campusride_db.* TO 'campusride'@'localhost';

-- MySQL 8 has binary logging ON by default; then a non-root user may only
-- create triggers if this flag is 1. (Needed by 01_schema.sql.)
-- PERSIST keeps the setting after MySQL restarts.
SET PERSIST log_bin_trust_function_creators = 1;
