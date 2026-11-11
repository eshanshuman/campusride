-- =====================================================================
--  CampusRide - constraint tests (optional, for the viva)
--  EVERY statement below is bad data and MUST be rejected by MySQL.
--  Run on the sample data with --force so it continues after each error:
--      mysql -u campusride -p --force campusride_db < sql/03_constraint_tests.sql
--  Each line should print an ERROR. Nothing is changed in the database.
-- =====================================================================

-- T1 duplicate username (UNIQUE)                         -> ERROR 1062
INSERT INTO users (username, password_hash, password_salt, role) VALUES ('riya', 'x', 'y', 'STUDENT');

-- T2 phone not 10 digits (CHECK)                         -> ERROR 3819
UPDATE students SET phone = '12345' WHERE student_id = 1;

-- T3 bad email (CHECK)                                   -> ERROR 3819
UPDATE students SET email = 'not-an-email' WHERE student_id = 1;

-- T4 duplicate roll number (UNIQUE)                      -> ERROR 1062
UPDATE students SET roll_no = 'RA2411004010102' WHERE student_id = 1;

-- T5 bus capacity zero (CHECK)                           -> ERROR 3819
INSERT INTO buses (reg_no, capacity) VALUES ('TN-11-XX-0000', 0);

-- T6 same bus on two routes (UNIQUE bus_id)              -> ERROR 1062
INSERT INTO routes (route_name, bus_id) VALUES ('R9 - Duplicate Bus', 1);

-- T7 two stops with the same sequence on a route         -> ERROR 1062
INSERT INTO route_stops (route_id, stop_name, stop_seq, pickup_time) VALUES (1, 'Clash Stop', 1, '07:00');

-- T8 valid_to not after valid_from (CHECK)               -> ERROR 3819
INSERT INTO pass_applications (student_id, stop_id, valid_from, valid_to, status)
VALUES (1, 1, '2027-01-10', '2027-01-01', 'SUBMITTED');

-- T9 application for a stop that does not exist (FK)     -> ERROR 1452
INSERT INTO pass_applications (student_id, stop_id, valid_from, valid_to) VALUES (1, 999, '2027-01-01', '2027-02-01');

-- T10 second pass for the same application (UNIQUE)     -> ERROR 1062
--     status CANCELLED so the triggers skip it and only the UNIQUE key is tested
INSERT INTO bus_passes (application_id, pass_no, valid_from, valid_to, status)
VALUES (1, 'CR-2026-00099', '2026-11-01', '2027-04-30', 'CANCELLED');

-- T11 duplicate pass number (UNIQUE)                     -> ERROR 1062
INSERT INTO bus_passes (application_id, pass_no, valid_from, valid_to) VALUES (4, 'CR-2026-00001', '2026-11-15', '2027-04-30');

-- T12 wrong pass number format (CHECK)                   -> ERROR 3819
INSERT INTO bus_passes (application_id, pass_no, valid_from, valid_to) VALUES (4, 'PASS-1', '2026-11-15', '2027-04-30');

-- T13 OVERBOOKING: route R3 has capacity 2 and already 2 active passes.
--     Karan's application 5 is for R3 (trigger)          -> ERROR 1644 'Bus is full ...'
INSERT INTO bus_passes (application_id, pass_no, valid_from, valid_to) VALUES (5, 'CR-2026-00090', '2026-11-15', '2027-04-30');

-- T14 OVERLAPPING ACTIVE PASS: Sneha (student 3) already has an active pass;
--     her rejected application 3 is reused here only to test the trigger -> ERROR 1644
INSERT INTO bus_passes (application_id, pass_no, valid_from, valid_to) VALUES (3, 'CR-2026-00091', '2026-12-01', '2027-01-31');

-- T15 deleting a non-DRAFT application (trigger)         -> ERROR 1644
DELETE FROM pass_applications WHERE application_id = 4;

-- T16 deleting an approved application that has a pass (trigger, and FK as backup) -> ERROR 1644
DELETE FROM pass_applications WHERE application_id = 1;

-- T17 deleting a bus that a route uses (FK)              -> ERROR 1451
DELETE FROM buses WHERE bus_id = 1;

-- T18 deleting a stop that applications refer to (FK)    -> ERROR 1451
DELETE FROM route_stops WHERE stop_id = 11;
