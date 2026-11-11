package com.campusride;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.campusride.dao.ApplicationDAO;
import com.campusride.dao.BusDAO;
import com.campusride.dao.PassDAO;
import com.campusride.dao.RouteDAO;
import com.campusride.dao.StopDAO;
import com.campusride.dao.StudentDAO;
import com.campusride.dao.UserDAO;
import com.campusride.model.Application;
import com.campusride.model.BusPass;
import com.campusride.model.Route;
import com.campusride.model.Student;
import com.campusride.model.User;
import com.campusride.util.DBConnection;
import com.campusride.util.PasswordUtil;

/**
 * Console smoke test for every DAO, run against the SAMPLE database.
 * (Plain main method - no test framework, so it is easy to read.)
 *
 * All writes happen inside one transaction that is ROLLED BACK at the end,
 * so the sample data is left exactly as it was.
 *
 * Run:  mvn -q test-compile exec:java -Dexec.mainClass=com.campusride.DaoSmokeTest -Dexec.classpathScope=test
 */
public class DaoSmokeTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        UserDAO userDAO = new UserDAO();
        StudentDAO studentDAO = new StudentDAO();
        BusDAO busDAO = new BusDAO();
        RouteDAO routeDAO = new RouteDAO();
        StopDAO stopDAO = new StopDAO();
        ApplicationDAO appDAO = new ApplicationDAO();
        PassDAO passDAO = new PassDAO();

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);   // everything below is undone at the end
            try {
                // ---- UserDAO ----
                User admin = userDAO.findByUsername(con, "admin");
                check("UserDAO.findByUsername(admin) is ADMIN", admin != null && admin.isAdmin());
                check("Demo password Admin@2026 verifies",
                      PasswordUtil.verify("Admin@2026".toCharArray(), admin.getPasswordSalt(), admin.getPasswordHash()));
                check("Wrong password is refused",
                      !PasswordUtil.verify("wrong".toCharArray(), admin.getPasswordSalt(), admin.getPasswordHash()));
                check("UserDAO.usernameExists(riya)", userDAO.usernameExists(con, "riya"));

                User u = new User();
                u.setUsername("smoke.user");
                u.setPasswordSalt(PasswordUtil.newSalt());
                u.setPasswordHash(PasswordUtil.hash("Smoke123".toCharArray(), u.getPasswordSalt()));
                u.setRole("STUDENT");
                int newUserId = userDAO.insert(con, u);
                check("UserDAO.insert returns new id", newUserId > 5);

                // ---- StudentDAO ----
                Student riya = studentDAO.findByUserId(con, 2);
                check("StudentDAO.findByUserId(2) = Riya", riya != null && riya.getFullName().startsWith("Riya"));
                check("StudentDAO.rollNoExists finds other student",
                      studentDAO.rollNoExists(con, "RA2411004010102", riya.getStudentId()));
                check("StudentDAO.rollNoExists ignores own roll no",
                      !studentDAO.rollNoExists(con, riya.getRollNo(), riya.getStudentId()));
                Student s = new Student();
                s.setUserId(newUserId);
                s.setRollNo("SMOKE0001");
                s.setFullName("Smoke Test");
                s.setDepartment("CSE");
                s.setYearOfStudy(1);
                s.setPhone("9999999999");
                s.setEmail("smoke@example.com");
                s.setStudentId(studentDAO.insert(con, s));
                s.setDepartment("ECE");
                check("StudentDAO.update", studentDAO.update(con, s) == 1
                      && "ECE".equals(studentDAO.findById(con, s.getStudentId()).getDepartment()));
                check("StudentDAO.lockById", studentDAO.lockById(con, s.getStudentId()));

                // ---- BusDAO / RouteDAO / StopDAO ----
                check("BusDAO.findAll has 3 buses", busDAO.findAll(con).size() == 3);
                busDAO.insert(con, "TN-11-SM-0001", 10);
                check("BusDAO.findUnassigned finds new bus", busDAO.findUnassigned(con).size() == 1);
                int newBusId = busDAO.findUnassigned(con).get(0).getBusId();
                check("BusDAO.updateCapacity", busDAO.updateCapacity(con, newBusId, 12) == 1);
                check("BusDAO.deleteIfUnused refuses bus 1 (in use)", busDAO.deleteIfUnused(con, 1) == 0);

                List<Route> routes = routeDAO.findActive(con);
                check("RouteDAO.findActive has 3 routes", routes.size() == 3);
                Route r3 = routeDAO.findByIdForUpdate(con, 3);
                check("RouteDAO.findByIdForUpdate(3) capacity 2", r3 != null && r3.getCapacity() == 2);
                routeDAO.insert(con, "R9 - Smoke Route", newBusId);
                Route r9 = routeDAO.findAll(con).stream()
                        .filter(r -> r.getRouteName().startsWith("R9")).findFirst().orElse(null);
                check("RouteDAO.insert + findAll", r9 != null);
                check("RouteDAO.setActive(false)", routeDAO.setActive(con, r9.getRouteId(), false) == 1);

                check("StopDAO.findByRoute(1) has 5 stops", stopDAO.findByRoute(con, 1).size() == 5);
                stopDAO.insert(con, r9.getRouteId(), "Smoke Stop", 1, LocalTime.of(8, 0));
                int smokeStopId = stopDAO.findByRoute(con, r9.getRouteId()).get(0).getStopId();
                check("StopDAO.findById", stopDAO.findById(con, smokeStopId) != null);
                check("StopDAO.deleteIfUnused refuses used stop 11", stopDAO.deleteIfUnused(con, 11) == 0);
                check("RouteDAO.deleteIfUnused refuses route with stops",
                      routeDAO.deleteIfUnused(con, r9.getRouteId()) == 0);
                check("StopDAO.deleteIfUnused deletes free stop", stopDAO.deleteIfUnused(con, smokeStopId) == 1);
                check("RouteDAO.deleteIfUnused deletes empty route", routeDAO.deleteIfUnused(con, r9.getRouteId()) == 1);
                check("BusDAO.deleteIfUnused deletes free bus", busDAO.deleteIfUnused(con, newBusId) == 1);

                // ---- ApplicationDAO ----
                check("ApplicationDAO.findByStatus(SUBMITTED) = 2", appDAO.findByStatus(con, "SUBMITTED").size() == 2);
                Application app5 = appDAO.findById(con, 5);
                check("ApplicationDAO.findById(5) route via stop = 3", app5 != null && app5.getRouteId() == 3);
                check("ApplicationDAO.lockAndGetStatus(4) = SUBMITTED",
                      "SUBMITTED".equals(appDAO.lockAndGetStatus(con, 4)));
                Application draft = new Application();
                draft.setStudentId(s.getStudentId());
                draft.setStopId(1);
                draft.setValidFrom(LocalDate.of(2027, 1, 1));
                draft.setValidTo(LocalDate.of(2027, 3, 31));
                draft.setStatus("DRAFT");
                int draftId = appDAO.insert(con, draft);
                check("ApplicationDAO.findByStudent sees draft", appDAO.findByStudent(con, s.getStudentId()).size() == 1);
                check("ApplicationDAO.deleteDraft refuses SUBMITTED app 4", appDAO.deleteDraft(con, 4, 2) == 0);
                check("ApplicationDAO.submitDraft", appDAO.submitDraft(con, draftId, s.getStudentId()) == 1);
                check("ApplicationDAO.countSubmittedOverlap = 1",
                      appDAO.countSubmittedOverlap(con, s.getStudentId(), LocalDate.of(2027, 2, 1),
                                                   LocalDate.of(2027, 2, 2), 0) == 1);
                check("ApplicationDAO.review REJECTED",
                      appDAO.review(con, draftId, "REJECTED", admin.getUserId(), "smoke") == 1);
                check("ApplicationDAO.review twice is refused",
                      appDAO.review(con, draftId, "APPROVED", admin.getUserId(), "again") == 0);

                // ---- PassDAO ----
                check("PassDAO.countActiveOverlapOnRoute(R3) = 2",
                      passDAO.countActiveOverlapOnRoute(con, 3, LocalDate.of(2026, 12, 1), LocalDate.of(2027, 1, 1)) == 2);
                check("PassDAO.countActiveOverlapForStudent(Riya) = 1",
                      passDAO.countActiveOverlapForStudent(con, 1, LocalDate.of(2026, 12, 1), LocalDate.of(2027, 1, 1)) == 1);
                check("PassDAO.countActiveOverlapForStudent outside dates = 0",
                      passDAO.countActiveOverlapForStudent(con, 1, LocalDate.of(2027, 6, 1), LocalDate.of(2027, 7, 1)) == 0);
                check("PassDAO.nextPassNo(2026) = CR-2026-00003", "CR-2026-00003".equals(passDAO.nextPassNo(con, 2026)));
                check("PassDAO.nextPassNo(2030) = CR-2030-00001", "CR-2030-00001".equals(passDAO.nextPassNo(con, 2030)));
                List<BusPass> riyaPasses = passDAO.findByStudent(con, 1);
                check("PassDAO.findByStudent(Riya) = 1 pass", riyaPasses.size() == 1
                      && "CR-2026-00001".equals(riyaPasses.get(0).getPassNo()));
                check("PassDAO.findPassengers(R3) = 2", passDAO.findPassengers(con, 3).size() == 2);
                check("PassDAO.cancel", passDAO.cancel(con, 1) == 1);
                check("PassDAO.findPassengers(R3) after cancel = 1", passDAO.findPassengers(con, 3).size() == 1);
                check("PassDAO.expireOldPasses runs", passDAO.expireOldPasses(con) >= 0);
            } finally {
                con.rollback();   // leave the sample data untouched
            }
        }

        System.out.println();
        System.out.println("DAO smoke test: " + passed + " passed, " + failed + " failed (all changes rolled back)");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "  PASS  " : "  FAIL  ") + name);
        if (ok) {
            passed++;
        } else {
            failed++;
        }
    }
}
