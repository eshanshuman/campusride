# CampusRide — College Bus Pass & Route Allocation System

Semester-3 project · Class AS-1 · SRM
Team: **Eshanshuman Singh, Neeti Soni, Avin Shrivastava**

A Java Swing desktop app where students register, browse bus routes, stops and timings, apply for a bus
pass for one stop and a date range, and track the status. The transport admin approves or rejects
applications with a capacity check, the system issues a pass, and the admin views passenger lists per
route and maintains buses, routes and stops.

**Tech:** Java 17 · Swing · MySQL 8.0.16+ · plain JDBC (`PreparedStatement` only) · Maven.
No Spring, no Hibernate/JPA and no external UI libraries. The only dependency is `mysql-connector-j`,
which Maven downloads (no JAR is committed).

---

## 1. Setup

### 1.1 Install
- **JDK 17** (any newer JDK also works; the build targets Java 17).
- **MySQL Server 8.0.16 or newer** (CHECK constraints are only enforced from 8.0.16).
- **Maven 3.9+**, or just use Eclipse / IntelliJ, which have Maven built in.

### 1.2 Create the database login (once, as MySQL `root`)
```
mysql -u root -p < sql/00_create_db_user.sql
```
This creates the user `campusride` / `CampusRide@123` with rights only on `campusride_db`, and sets
`log_bin_trust_function_creators = 1`. MySQL 8 has binary logging on by default, and without this
setting a non-root user cannot create the triggers in the schema.
(In MySQL Workbench you can instead open the file and run it with the lightning-bolt button.)

### 1.3 Create the tables and sample data
```
mysql -u campusride -p < sql/01_schema.sql
mysql -u campusride -p < sql/02_sample_data.sql
```
Run both again at any time to **reset** to the demo data (`01_schema.sql` drops and recreates the database).

Optional, for the viva: `sql/03_constraint_tests.sql` contains 18 bad statements, and MySQL must reject
every one of them.
```
mysql -u campusride -p --force campusride_db < sql/03_constraint_tests.sql
```

### 1.4 Database settings
Copy `src/main/resources/db.properties.example` to `src/main/resources/db.properties` and enter your
password. `db.properties` is in `.gitignore`, so passwords never reach GitHub.

### 1.5 Run
- **IDE:** import as a Maven project, then run `com.campusride.Main`.
  (Eclipse: *File → Import → Existing Maven Projects*. IntelliJ: *File → Open → select the folder*.)
- **Command line:** `mvn compile exec:java`

---

## 2. Demo logins

| Username | Password      | Role    | Situation in the sample data                                   |
|----------|---------------|---------|----------------------------------------------------------------|
| `admin`  | `Admin@2026`  | ADMIN   | Transport office                                               |
| `riya`   | `Student@123` | STUDENT | ACTIVE pass CR-2026-00001 on R3                                 |
| `arjun`  | `Student@123` | STUDENT | SUBMITTED application on R1, waiting for approval              |
| `sneha`  | `Student@123` | STUDENT | ACTIVE pass CR-2026-00002 on R3, plus an older REJECTED one    |
| `karan`  | `Student@123` | STUDENT | SUBMITTED on R3 (bus full, so approval is refused), a DRAFT on R2 |

All people in the sample data are fictional. The passwords are stored as real PBKDF2 hashes, made with
`PasswordUtil.main`. The sample passes run from 2026-11-01 to 2027-04-30.

---

## 3. Screens (7)

| # | Class | What it does |
|---|-------|--------------|
| 1 | `LoginFrame` | Login tab and student Register tab |
| 2 | `StudentDashboard` | Student window: header, Logout and four tabs (screens 3–6) |
| 3 | `RoutesPanel` | Routes, stops and pickup times; **seats left** for a chosen period; *Apply for this stop* |
| 4 | `ApplicationPanel` | Apply (route → stop, dates, remarks) as DRAFT or SUBMITTED; status list; submit or delete drafts |
| 5 | `PassViewPanel` | Bus-pass "card" for the ACTIVE pass and a list of all passes |
| 6 | `ProfilePanel` | View and edit your profile |
| 7 | `AdminDashboard` | Tabs: **Approvals** (with capacity panel), **Routes & Buses**, **Passenger Lists** |

`UiUtil` holds the shared colours (navy `#173B63`, steel blue `#2F6DB2`, yellow `#F4C542`) and small
layout and message helpers.

---

## 4. Architecture

```
ui/       Swing screens   -> call services only, show friendly JOptionPane messages
service/  rules + checks  -> validation, business rules, transactions, friendly errors
dao/      SQL only        -> one class per table, PreparedStatement + try-with-resources
model/    plain classes   -> one per table (plus a few display fields filled by JOINs)
util/     DBConnection, PasswordUtil, Session
```
- Every DAO method receives the `Connection` from the service. That way a service can run several DAO
  calls in **one transaction** on the same connection.
- Errors: DAOs throw `SQLException`. Services turn it into a `ServiceException` with a friendly sentence
  (`ServiceException.fromSql`: 1062 duplicate → e.g. "That roll number is already registered").
  Screens only ever show that sentence, never a stack trace.

---

## 5. Database (ER summary)

```
users 1───1 students 1───* pass_applications *───1 route_stops *───1 routes 1───1 buses
  │                              │ 1
  └──(reviewed_by) 0..*          └───0..1 bus_passes
```

| Table | Key points |
|-------|-----------|
| `users` | `username` UNIQUE, PBKDF2 `password_hash` + `password_salt`, `role` ENUM(STUDENT, ADMIN) |
| `students` | `user_id` FK UNIQUE (1:1), `roll_no` UNIQUE, CHECK phone = 10 digits, CHECK email pattern, CHECK year 1–5 |
| `buses` | `reg_no` UNIQUE, CHECK `capacity > 0` |
| `routes` | `route_name` UNIQUE, `bus_id` FK **UNIQUE** (one bus per route), `is_active` |
| `route_stops` | FK route, UNIQUE(`route_id`, `stop_seq`), `pickup_time` |
| `pass_applications` | FK student, FK **stop**, dates with CHECK `valid_to > valid_from`, `status` ENUM(DRAFT, SUBMITTED, APPROVED, REJECTED), `reviewed_by` FK users NULL |
| `bus_passes` | `application_id` FK **UNIQUE** (one pass per application), `pass_no` UNIQUE + CHECK format `CR-YYYY-00001`, `status` ENUM(ACTIVE, CANCELLED, EXPIRED) |

- **stop → route → bus:** the application stores only the stop. The route and bus are found by JOINs,
  so they are never stored twice.
- **An ACTIVE pass *is* the route allocation.** There is no separate allocation table.
- **Triggers (safety net):** `trg_pass_before_insert` and `trg_pass_before_update` refuse an ACTIVE pass
  that would overbook the bus or give a student two overlapping ACTIVE passes. `trg_app_before_delete`
  allows deleting only DRAFT applications. Foreign keys block deleting buses, routes and stops that are
  still in use.
- **Overlap rule:** two periods overlap when `from1 <= to2 AND to1 >= from2`. Seats left for a period
  = capacity − ACTIVE passes on the route overlapping that period. This is simple and conservative, so
  the bus can never be overbooked.

---

## 6. Business rules and the approval transaction

| Action | SQL | Rule |
|--------|-----|------|
| Register | INSERT users + students (one transaction) | All fields required; unique username and roll no; phone 10 digits; valid email; password 8+ chars with a letter and a digit |
| Edit profile | UPDATE students | Same field rules; roll no and username cannot change |
| Apply | INSERT pass_applications | A stop must be chosen; `valid_to` after `valid_from`; start not in the past; at most one year. Submitting is also refused if the student already has an overlapping ACTIVE pass or a pending application, or if the bus is full |
| Delete | DELETE | Only your own DRAFT applications; also unreferenced stops/routes/buses. Approved applications and passes are never deleted (passes are CANCELLED instead) |
| Approve | **one transaction** (below) | Capacity and overlap are re-checked while holding row locks |
| Reject | UPDATE | Only SUBMITTED; a reason is required |

`AdminService.approve()`:
1. `setAutoCommit(false)` (with READ COMMITTED isolation, so the counts see the latest committed passes)
2. `SELECT … FROM routes … WHERE route_id = ? FOR UPDATE` locks the route (and its bus)
3. `SELECT … FROM students WHERE student_id = ? FOR UPDATE` locks the student
4. Re-checks: the application is still SUBMITTED, the route is active, the student has no overlapping
   ACTIVE pass, and the ACTIVE passes on the route overlapping the dates are fewer than the bus capacity
5. `UPDATE pass_applications SET status='APPROVED' …`
6. `INSERT INTO bus_passes …` with the next `CR-YYYY-NNNNN` number
7. `commit()`. On any failure: `rollback()`, and the admin sees why ("Bus TN-11-CR-1003 is full for this
   period (2 of 2 seats taken)").

Because the route row is locked, two admins approving the last seat at the same moment are handled one
after the other. The second one sees the bus is full and is refused. `ServiceTest` checks this with two
threads.

---

## 7. Planned demo (about 5 minutes)

1. **Reset data:** run `01_schema.sql` and `02_sample_data.sql`.
2. **Register a student** on the Register tab (try a 9-digit phone first to show validation), then log in.
3. **Routes & Timings:** pick *R2 – Velachery Line*, check seats for the period, select a stop, and click
   **Apply for this stop**. The form opens pre-filled. Click **Submit application**.
4. Show the status **SUBMITTED** in *My applications*. Log out.
5. Log in as **admin → Approvals**. Select the new application; the capacity panel shows capacity 35,
   seats left and "OK to approve". Click **Approve & issue pass**, and pass `CR-2026-00003` is issued.
6. Select **Karan's** application (R3). The panel shows *Cannot approve: bus is full*. Click Approve
   anyway to show the transaction refuse it and roll back.
7. **Passenger Lists → R2** now shows the new student at their stop.
8. Log back in as the student. **My Bus Pass** shows the ACTIVE pass card.

---

## 8. Tests

| What | How | Result when last run |
|------|-----|----------------------|
| Constraints reject bad data | `mysql … --force campusride_db < sql/03_constraint_tests.sql` | 18/18 rejected |
| Every DAO method against the sample DB (rolled back) | `mvn -q test-compile exec:java -Dexec.mainClass=com.campusride.DaoSmokeTest -Dexec.classpathScope=test` | 44/44 pass |
| Services and approval transaction (**changes data**, so reset afterwards) | `mvn -q test-compile exec:java -Dexec.mainClass=com.campusride.ServiceTest -Dexec.classpathScope=test` | 26/26 pass |
| Every screen opens and loads data (PNGs in `target/screenshots/`) | `mvn -q test-compile exec:java -Dexec.mainClass=com.campusride.ScreenSnapshotTest -Dexec.classpathScope=test` | all 10 views rendered |

These are plain `main` methods with no test framework, so they are easy to read.

---

## 9. Scope

**In version 1:** everything above. **Out of scope:** online payment, GPS tracking, route optimisation,
email/SMS. Assumptions: one bus per route with a fixed capacity; one ACTIVE pass per student for any
overlapping period.

**Troubleshooting:** *"Cannot connect to the database"* means MySQL is not running or `db.properties` is
wrong. To check that MySQL is running on Windows, open `services.msc` and look for **MySQL80**.
