# CampusRide - College Bus Pass & Route Allocation System

Semester 3 Java project, class AS-1, SRM.

**Team:** Eshanshuman Singh, Neeti Soni, Avin Shrivastava

## What it is

CampusRide is a desktop app for managing college bus passes.

- **Students** can register, look at the bus routes, stops and pickup times, apply for a bus pass for
  one stop and a date range, and check the status of their application.
- **The transport admin** approves or rejects applications. Before approving, the app checks that the
  bus still has a free seat for those dates. When an application is approved, a pass number like
  `CR-2026-00003` is generated. The admin can also see the passenger list of each route and add or
  remove buses, routes and stops.

Some rules we followed:
- Each route has exactly one bus, and each bus has a fixed number of seats.
- A student can't have two ACTIVE passes for dates that overlap.
- An approval can never put more students on a bus than it has seats.

Not included: online payment, GPS tracking, route optimisation, email/SMS.

## Tools used

- Java 17 with Swing for the screens
- MySQL 8 (8.0.16 or newer, because older versions ignore CHECK constraints)
- JDBC with PreparedStatement for all the SQL
- Maven to build the project and download the MySQL driver (`mysql-connector-j`)

We did not use any framework (no Spring, no Hibernate).

## Setting up MySQL

You only need to do this once on each laptop.

**1. Create the database user.** Log in to MySQL as root and run `sql/00_create_db_user.sql`.
You can open it in MySQL Workbench and run it, or use the command line:
```
mysql -u root -p < sql/00_create_db_user.sql
```
This creates a user `campusride` with password `CampusRide@123` that can only use `campusride_db`.
It also turns on a setting MySQL needs before a normal user can create triggers.

**2. Create the tables and add the sample data:**
```
mysql -u campusride -p < sql/01_schema.sql
mysql -u campusride -p < sql/02_sample_data.sql
```
`01_schema.sql` deletes and creates the whole database again, so running these two files is also how
we reset the data before a demo.

**3. Add your database settings.** Copy `src/main/resources/db.properties.example` to
`src/main/resources/db.properties` and put your password in it. This file is in `.gitignore`, so it
does not get uploaded to GitHub.

## How to run

- **VS Code:** install the "Extension Pack for Java". Open the project folder, open
  `src/main/java/com/campusride/Main.java` and click **Run**.
- **Eclipse:** File → Import → Existing Maven Projects, then run `Main.java`.
- **IntelliJ:** File → Open → choose the project folder, then run `Main.java`.
- **Command line (needs Maven):** `mvn compile exec:java`

If you see "Cannot connect to the database", MySQL is probably not running. On Windows, open
`services.msc`, find **MySQL80** and start it.

## Demo logins

| Username | Password    | Role    | In the sample data                                        |
|----------|-------------|---------|-----------------------------------------------------------|
| admin    | Admin@2026  | Admin   | transport office                                          |
| riya     | Student@123 | Student | has an ACTIVE pass on route R3                            |
| arjun    | Student@123 | Student | has a SUBMITTED application on R1 (approving it works)    |
| sneha    | Student@123 | Student | has an ACTIVE pass on R3 and an old REJECTED application  |
| karan    | Student@123 | Student | SUBMITTED on R3, but R3 is full, so approving it fails; also has a DRAFT |

All the people in the sample data are made up. Route R3's bus has only 2 seats on purpose, so we can
show what happens when a bus is full. The sample passes are valid from 2026-11-01 to 2027-04-30.

The passwords are not stored as plain text. We store a PBKDF2 hash and a random salt
(see `PasswordUtil.java`). We made the sample hashes by running `PasswordUtil.main`.

## Screens

1. **LoginFrame** - Login tab and Register tab
2. **StudentDashboard** - the student's main window with 4 tabs:
3. **RoutesPanel** - routes, stops, pickup times and seats left, plus an "Apply for this stop" button
4. **ApplicationPanel** - apply for a pass (save as draft or submit), and see the status of your applications
5. **PassViewPanel** - your bus pass shown as a card, and a list of all your passes
6. **ProfilePanel** - view and edit your profile
7. **AdminDashboard** - the admin's window with 3 tabs: Approvals, Routes & Buses, Passenger Lists

## How the code is organised

| Package   | What is in it                                                                   |
|-----------|---------------------------------------------------------------------------------|
| `ui`      | the Swing screens. They only call the service classes.                          |
| `service` | checks and rules (validation, capacity check, the approval transaction)         |
| `dao`     | the SQL. One DAO class for each table.                                          |
| `model`   | simple classes with fields, getters and setters, one for each table             |
| `util`    | `DBConnection`, `PasswordUtil`, `Session` (who is logged in)                    |

If something goes wrong, the service throws a `ServiceException` with a simple message, and the screen
shows it in a JOptionPane.

## Database

There are 7 tables:
`users`, `students`, `buses`, `routes`, `route_stops`, `pass_applications`, `bus_passes`.

```
users 1---1 students 1---* pass_applications *---1 route_stops *---1 routes 1---1 buses
                                    |
                                    1---0..1 bus_passes
```

- An application only stores the **stop**. We get the route and the bus through the stop
  (stop -> route -> bus), so the route is not saved twice.
- An **ACTIVE pass is the route allocation**. There is no separate allocation table.
- **Constraints we used:**
  - UNIQUE: username, roll no, bus reg no, route name, bus_id in routes, pass_no, and one pass per application
  - CHECK: phone is 10 digits, email format, capacity > 0, valid_to > valid_from, and the pass number format
  - Foreign keys between all the tables
- **Triggers (3):** two triggers on `bus_passes` stop an ACTIVE pass from being added if the bus is
  full or the student already has an overlapping pass. One trigger on `pass_applications` only allows
  deleting DRAFT applications. The Java code checks these rules too; the triggers are an extra safety
  net in the database itself.

**Two date ranges overlap when** `from1 <= to2 AND to1 >= from2`.
**Seats left** = bus capacity - ACTIVE passes on that route that overlap the dates.

## The approval transaction

`AdminService.approve()` does everything in **one transaction**:

1. `setAutoCommit(false)`
2. `SELECT ... FOR UPDATE` on the route row, so two admins can't take the last seat at the same time
3. `SELECT ... FOR UPDATE` on the student row
4. Check again that:
   - the application is still SUBMITTED
   - the route is active
   - the student has no overlapping ACTIVE pass
   - the bus is not full for those dates
5. Update the application to APPROVED
6. Insert the new pass with the next pass number
7. `commit()`

If anything fails, we `rollback()`, so nothing is half-saved, and the admin sees the reason.

## Demo plan

1. Reset the data (run `01_schema.sql` and `02_sample_data.sql`).
2. Register a new student. First try a 9-digit phone number to show the validation message.
3. Log in as that student. Go to **Routes & Timings**, pick R2, choose a stop and click
   **Apply for this stop**. Submit the application.
4. The application shows as SUBMITTED. Log out.
5. Log in as **admin**. In **Approvals**, select the new application. The capacity box shows the seats
   left. Click **Approve & issue pass**.
6. Select Karan's application (R3). The box says the bus is full. Clicking Approve shows the error,
   and nothing is saved.
7. **Passenger Lists → R2** now shows the new student.
8. Log in as the student again. **My Bus Pass** shows the pass.

## Tests

We test with small programs that have a `main` method (no testing framework):

| Test | What it checks | Result |
|------|----------------|--------|
| `sql/03_constraint_tests.sql` | 18 wrong INSERT/UPDATE/DELETE statements, MySQL must reject all of them | 18 rejected |
| `DaoSmokeTest` | every DAO method on the sample data (everything is rolled back at the end) | 44 / 44 |
| `ServiceTest` | services and the approval transaction, including two admins approving the last seat at the same time (this one changes the data, so reset afterwards) | 26 / 26 |
| `ScreenSnapshotTest` | opens every screen and saves a picture of it in `target/screenshots/` | all screens |

How to run them:
```
mysql -u campusride -p --force campusride_db < sql/03_constraint_tests.sql
mvn -q test-compile exec:java -Dexec.mainClass=com.campusride.DaoSmokeTest -Dexec.classpathScope=test
mvn -q test-compile exec:java -Dexec.mainClass=com.campusride.ServiceTest -Dexec.classpathScope=test
mvn -q test-compile exec:java -Dexec.mainClass=com.campusride.ScreenSnapshotTest -Dexec.classpathScope=test
```
