-- =====================================================================
--  CampusRide - File 2 of 2 : sample (demo) data
--  All people below are FICTIONAL. Run AFTER 01_schema.sql:
--      mysql -u <user> -p < sql/02_sample_data.sql
--
--  Demo logins (password hashes were made with PasswordUtil.main):
--      admin    / Admin@2026    (ADMIN)
--      riya     / Student@123   (STUDENT)
--      arjun    / Student@123   (STUDENT)
--      sneha    / Student@123   (STUDENT)
--      karan    / Student@123   (STUDENT)
-- =====================================================================
USE campusride_db;

-- ---------- users (ids 1..5) ----------
INSERT INTO users (user_id, username, password_hash, password_salt, role) VALUES
 (1, 'admin', 'IrBfuSoVpxBv1CEqaMaSz1ncFejSgIcAwtDIFA9i020=', 'Y2uf2kB+2sZaBjh+i6gYlA==', 'ADMIN'),
 (2, 'riya',  'AoMGWcwvefCMhyVGnurWC29dLm7TemAe11thyTq22sQ=', '7gbHRCV+98kK3J1d89YLyA==', 'STUDENT'),
 (3, 'arjun', 'vpNGxQOI51G5XdvlTfQO8hUou5B8G74dpy1rSkRz76k=', 'TOAT99faugvF+YsvaEw6nA==', 'STUDENT'),
 (4, 'sneha', 'gE9U1agNe83KNJOpw7m75vzHsGIEmYgCV+fUTbJ7NPg=', 'sLWPYqwg/Hjb7mMnGnmC1A==', 'STUDENT'),
 (5, 'karan', 'o61GizOJNI6tUsPSPAjpF61/XKm1J/bIF1AcpLv5v3M=', '8k3S1lQbKF9YL2XK3ilQNA==', 'STUDENT');

-- ---------- students (ids 1..4) ----------
INSERT INTO students (student_id, user_id, roll_no, full_name, department, year_of_study, phone, email) VALUES
 (1, 2, 'RA2411003010101', 'Riya Kapoor',  'CSE',  2, '9000000001', 'riya.demo@example.com'),
 (2, 3, 'RA2411004010102', 'Arjun Mehta',  'ECE',  2, '9000000002', 'arjun.demo@example.com'),
 (3, 4, 'RA2411002010103', 'Sneha Iyer',   'MECH', 3, '9000000003', 'sneha.demo@example.com'),
 (4, 5, 'RA2411008010104', 'Karan Joshi',  'IT',   1, '9000000004', 'karan.demo@example.com');

-- ---------- buses ----------
-- Bus 3 deliberately has capacity 2 so the "bus full" rule can be demonstrated.
INSERT INTO buses (bus_id, reg_no, capacity) VALUES
 (1, 'TN-11-CR-1001', 40),
 (2, 'TN-11-CR-1002', 35),
 (3, 'TN-11-CR-1003', 2);

-- ---------- routes (one bus each) ----------
INSERT INTO routes (route_id, route_name, bus_id, is_active) VALUES
 (1, 'R1 - Tambaram Line',       1, TRUE),
 (2, 'R2 - Velachery Line',      2, TRUE),
 (3, 'R3 - Guduvanchery Mini',   3, TRUE);

-- ---------- stops ----------
INSERT INTO route_stops (stop_id, route_id, stop_name, stop_seq, pickup_time) VALUES
 -- R1
 (1,  1, 'Tambaram Bus Stand',   1, '07:00:00'),
 (2,  1, 'Perungalathur',        2, '07:10:00'),
 (3,  1, 'Vandalur Zoo Gate',    3, '07:20:00'),
 (4,  1, 'Urapakkam',            4, '07:30:00'),
 (5,  1, 'Campus Main Gate',     5, '07:50:00'),
 -- R2
 (6,  2, 'Velachery MRTS',       1, '06:50:00'),
 (7,  2, 'Medavakkam Junction',  2, '07:05:00'),
 (8,  2, 'Selaiyur',             3, '07:20:00'),
 (9,  2, 'Campus Main Gate',     4, '07:55:00'),
 -- R3
 (10, 3, 'Guduvanchery Station', 1, '07:25:00'),
 (11, 3, 'Nandhivaram',          2, '07:32:00'),
 (12, 3, 'Potheri Signal',       3, '07:40:00'),
 (13, 3, 'Campus Main Gate',     4, '07:45:00');

-- ---------- applications (every status appears) ----------
-- Semester period used in the demo: 2026-11-01 .. 2027-04-30
INSERT INTO pass_applications
 (application_id, student_id, stop_id, valid_from, valid_to, status, remarks,
  applied_at, reviewed_by, reviewed_at, admin_remarks) VALUES
 -- 1: Riya, R3, approved -> pass CR-2026-00001
 (1, 1, 11, '2026-11-01', '2027-04-30', 'APPROVED', 'Lives near Nandhivaram',
  '2026-10-20 10:00:00', 1, '2026-10-22 11:00:00', 'Approved'),
 -- 2: Sneha, R3, approved -> pass CR-2026-00002  (R3 is now FULL: 2 of 2 seats)
 (2, 3, 10, '2026-11-01', '2027-04-30', 'APPROVED', NULL,
  '2026-10-21 09:30:00', 1, '2026-10-22 11:05:00', 'Approved'),
 -- 3: Sneha, older request for R2, rejected
 (3, 3, 7,  '2026-11-01', '2027-04-30', 'REJECTED', 'Would prefer Medavakkam',
  '2026-10-18 08:00:00', 1, '2026-10-19 12:00:00', 'Duplicate request - applied again for R3'),
 -- 4: Arjun, R1, waiting for admin  -> demo: approve succeeds
 (4, 2, 3,  '2026-11-15', '2027-04-30', 'SUBMITTED', 'Daily commute from Vandalur',
  '2026-11-05 14:00:00', NULL, NULL, NULL),
 -- 5: Karan, R3, waiting for admin  -> demo: approve fails, bus full
 (5, 4, 12, '2026-11-15', '2027-04-30', 'SUBMITTED', NULL,
  '2026-11-06 15:00:00', NULL, NULL, NULL),
 -- 6: Karan, R2, still a draft (can be edited / deleted by Karan)
 (6, 4, 8,  '2026-12-01', '2027-04-30', 'DRAFT', 'Maybe switch to R2',
  '2026-11-07 16:00:00', NULL, NULL, NULL);

-- ---------- passes issued for the two approved applications ----------
INSERT INTO bus_passes (pass_id, application_id, pass_no, valid_from, valid_to, status, issued_at) VALUES
 (1, 1, 'CR-2026-00001', '2026-11-01', '2027-04-30', 'ACTIVE', '2026-10-22 11:00:00'),
 (2, 2, 'CR-2026-00002', '2026-11-01', '2027-04-30', 'ACTIVE', '2026-10-22 11:05:00');
