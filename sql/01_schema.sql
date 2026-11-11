-- =====================================================================
--  CampusRide - College Bus Pass & Route Allocation System
--  File 1 of 2 : database schema (tables, constraints, triggers)
--  Requires MySQL 8.0.16 or newer (CHECK constraints are enforced from 8.0.16).
--
--  Run:  mysql -u <user> -p < sql/01_schema.sql
--  WARNING: this drops and recreates campusride_db (all data is lost).
-- =====================================================================

DROP DATABASE IF EXISTS campusride_db;
CREATE DATABASE campusride_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE campusride_db;

-- ---------------------------------------------------------------------
-- 1. users : one row per login (students AND admins)
--    Passwords are never stored in plain text: we store a PBKDF2 hash
--    and the random salt used to make it (see PasswordUtil.java).
-- ---------------------------------------------------------------------
CREATE TABLE users (
    user_id        INT AUTO_INCREMENT PRIMARY KEY,
    username       VARCHAR(50)  NOT NULL UNIQUE,
    password_hash  VARCHAR(100) NOT NULL,
    password_salt  VARCHAR(50)  NOT NULL,
    role           ENUM('STUDENT','ADMIN') NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 2. students : profile details, exactly one per STUDENT user (1 : 1)
-- ---------------------------------------------------------------------
CREATE TABLE students (
    student_id     INT AUTO_INCREMENT PRIMARY KEY,
    user_id        INT          NOT NULL UNIQUE,          -- UNIQUE makes it 1:1
    roll_no        VARCHAR(20)  NOT NULL UNIQUE,
    full_name      VARCHAR(80)  NOT NULL,
    department     VARCHAR(50)  NOT NULL,
    year_of_study  TINYINT      NOT NULL,
    phone          CHAR(10)     NOT NULL,
    email          VARCHAR(100) NOT NULL,
    CONSTRAINT fk_students_user   FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT chk_students_year  CHECK (year_of_study BETWEEN 1 AND 5),
    CONSTRAINT chk_students_phone CHECK (phone REGEXP '^[0-9]{10}$'),
    CONSTRAINT chk_students_email CHECK (email LIKE '%_@_%._%')
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 3. buses : the physical vehicles
-- ---------------------------------------------------------------------
CREATE TABLE buses (
    bus_id    INT AUTO_INCREMENT PRIMARY KEY,
    reg_no    VARCHAR(15) NOT NULL UNIQUE,
    capacity  INT         NOT NULL,
    CONSTRAINT chk_buses_capacity CHECK (capacity > 0)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 4. routes : one bus per route (bus_id is UNIQUE, so a bus serves
--    at most one route)
-- ---------------------------------------------------------------------
CREATE TABLE routes (
    route_id    INT AUTO_INCREMENT PRIMARY KEY,
    route_name  VARCHAR(60) NOT NULL UNIQUE,
    bus_id      INT         NOT NULL UNIQUE,
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_routes_bus FOREIGN KEY (bus_id) REFERENCES buses(bus_id)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 5. route_stops : ordered pickup points on a route
-- ---------------------------------------------------------------------
CREATE TABLE route_stops (
    stop_id      INT AUTO_INCREMENT PRIMARY KEY,
    route_id     INT         NOT NULL,
    stop_name    VARCHAR(80) NOT NULL,
    stop_seq     INT         NOT NULL,
    pickup_time  TIME        NOT NULL,
    CONSTRAINT fk_stops_route   FOREIGN KEY (route_id) REFERENCES routes(route_id),
    CONSTRAINT uq_stops_seq     UNIQUE (route_id, stop_seq),
    CONSTRAINT chk_stops_seq    CHECK (stop_seq > 0)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 6. pass_applications : a student asks for a pass for ONE stop.
--    The stop tells us the route, and the route tells us the bus
--    (stop -> route -> bus), so route_id is deliberately NOT stored here.
-- ---------------------------------------------------------------------
CREATE TABLE pass_applications (
    application_id  INT AUTO_INCREMENT PRIMARY KEY,
    student_id      INT          NOT NULL,
    stop_id         INT          NOT NULL,
    valid_from      DATE         NOT NULL,
    valid_to        DATE         NOT NULL,
    status          ENUM('DRAFT','SUBMITTED','APPROVED','REJECTED') NOT NULL DEFAULT 'DRAFT',
    remarks         VARCHAR(255) NULL,               -- student's note
    applied_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_by     INT          NULL,               -- admin user who decided
    reviewed_at     TIMESTAMP    NULL,
    admin_remarks   VARCHAR(255) NULL,
    CONSTRAINT fk_app_student  FOREIGN KEY (student_id)  REFERENCES students(student_id),
    CONSTRAINT fk_app_stop     FOREIGN KEY (stop_id)     REFERENCES route_stops(stop_id),
    CONSTRAINT fk_app_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(user_id),
    CONSTRAINT chk_app_dates   CHECK (valid_to > valid_from)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 7. bus_passes : issued when an application is APPROVED.
--    An ACTIVE pass IS the route allocation (no separate table).
--    application_id is UNIQUE -> one application can never get two passes.
-- ---------------------------------------------------------------------
CREATE TABLE bus_passes (
    pass_id         INT AUTO_INCREMENT PRIMARY KEY,
    application_id  INT          NOT NULL UNIQUE,
    pass_no         VARCHAR(20)  NOT NULL UNIQUE,    -- e.g. CR-2026-00001
    valid_from      DATE         NOT NULL,
    valid_to        DATE         NOT NULL,
    status          ENUM('ACTIVE','CANCELLED','EXPIRED') NOT NULL DEFAULT 'ACTIVE',
    issued_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pass_app      FOREIGN KEY (application_id) REFERENCES pass_applications(application_id),
    CONSTRAINT chk_pass_dates   CHECK (valid_to > valid_from),
    CONSTRAINT chk_pass_no      CHECK (pass_no REGEXP '^CR-[0-9]{4}-[0-9]{5}$')
) ENGINE = InnoDB;

-- Helpful indexes for the overlap / passenger-list queries
CREATE INDEX idx_app_student_status ON pass_applications (student_id, status);
CREATE INDEX idx_pass_status_dates  ON bus_passes (status, valid_from, valid_to);

-- =====================================================================
--  TRIGGERS  (a safety net in the database itself)
--  The Java code already checks these rules inside a transaction.
--  The triggers make sure that even a manual INSERT/UPDATE in MySQL
--  Workbench cannot overbook a bus or give a student two passes.
--
--  Two date ranges [a1, a2] and [b1, b2] overlap when  a1 <= b2 AND a2 >= b1.
-- =====================================================================
DELIMITER $$

-- Rules checked before an ACTIVE pass is inserted
CREATE TRIGGER trg_pass_before_insert
BEFORE INSERT ON bus_passes
FOR EACH ROW
BEGIN
    DECLARE v_student   INT;
    DECLARE v_route     INT;
    DECLARE v_capacity  INT;
    DECLARE v_count     INT;

    IF NEW.status = 'ACTIVE' THEN
        -- Find the student and route for this application (stop -> route -> bus)
        SELECT a.student_id, s.route_id, b.capacity
          INTO v_student, v_route, v_capacity
          FROM pass_applications a
          JOIN route_stops s ON s.stop_id  = a.stop_id
          JOIN routes      r ON r.route_id = s.route_id
          JOIN buses       b ON b.bus_id   = r.bus_id
         WHERE a.application_id = NEW.application_id;

        -- Rule 1: a student may hold only one ACTIVE pass for any overlapping period
        SELECT COUNT(*) INTO v_count
          FROM bus_passes p
          JOIN pass_applications a ON a.application_id = p.application_id
         WHERE a.student_id = v_student
           AND p.status = 'ACTIVE'
           AND p.valid_from <= NEW.valid_to
           AND p.valid_to   >= NEW.valid_from;
        IF v_count > 0 THEN
            SIGNAL SQLSTATE '45000'
               SET MESSAGE_TEXT = 'Student already has an ACTIVE pass for an overlapping period';
        END IF;

        -- Rule 2: ACTIVE passes on this route overlapping the period must stay below capacity
        SELECT COUNT(*) INTO v_count
          FROM bus_passes p
          JOIN pass_applications a ON a.application_id = p.application_id
          JOIN route_stops s       ON s.stop_id = a.stop_id
         WHERE s.route_id = v_route
           AND p.status = 'ACTIVE'
           AND p.valid_from <= NEW.valid_to
           AND p.valid_to   >= NEW.valid_from;
        IF v_count >= v_capacity THEN
            SIGNAL SQLSTATE '45000'
               SET MESSAGE_TEXT = 'Bus is full for the requested period';
        END IF;
    END IF;
END$$

-- Same rules when an existing pass is switched back to ACTIVE
CREATE TRIGGER trg_pass_before_update
BEFORE UPDATE ON bus_passes
FOR EACH ROW
BEGIN
    DECLARE v_student   INT;
    DECLARE v_route     INT;
    DECLARE v_capacity  INT;
    DECLARE v_count     INT;

    IF NEW.status = 'ACTIVE' AND OLD.status <> 'ACTIVE' THEN
        SELECT a.student_id, s.route_id, b.capacity
          INTO v_student, v_route, v_capacity
          FROM pass_applications a
          JOIN route_stops s ON s.stop_id  = a.stop_id
          JOIN routes      r ON r.route_id = s.route_id
          JOIN buses       b ON b.bus_id   = r.bus_id
         WHERE a.application_id = NEW.application_id;

        SELECT COUNT(*) INTO v_count
          FROM bus_passes p
          JOIN pass_applications a ON a.application_id = p.application_id
         WHERE a.student_id = v_student
           AND p.pass_id <> NEW.pass_id
           AND p.status = 'ACTIVE'
           AND p.valid_from <= NEW.valid_to
           AND p.valid_to   >= NEW.valid_from;
        IF v_count > 0 THEN
            SIGNAL SQLSTATE '45000'
               SET MESSAGE_TEXT = 'Student already has an ACTIVE pass for an overlapping period';
        END IF;

        SELECT COUNT(*) INTO v_count
          FROM bus_passes p
          JOIN pass_applications a ON a.application_id = p.application_id
          JOIN route_stops s       ON s.stop_id = a.stop_id
         WHERE s.route_id = v_route
           AND p.pass_id <> NEW.pass_id
           AND p.status = 'ACTIVE'
           AND p.valid_from <= NEW.valid_to
           AND p.valid_to   >= NEW.valid_from;
        IF v_count >= v_capacity THEN
            SIGNAL SQLSTATE '45000'
               SET MESSAGE_TEXT = 'Bus is full for the requested period';
        END IF;
    END IF;
END$$

-- Only DRAFT applications may ever be deleted
CREATE TRIGGER trg_app_before_delete
BEFORE DELETE ON pass_applications
FOR EACH ROW
BEGIN
    IF OLD.status <> 'DRAFT' THEN
        SIGNAL SQLSTATE '45000'
           SET MESSAGE_TEXT = 'Only DRAFT applications can be deleted';
    END IF;
END$$

DELIMITER ;
