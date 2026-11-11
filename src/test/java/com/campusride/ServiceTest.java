package com.campusride;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import com.campusride.model.Student;
import com.campusride.service.AdminService;
import com.campusride.service.ApplicationService;
import com.campusride.service.AuthService;
import com.campusride.service.ServiceException;
import com.campusride.util.DBConnection;

/**
 * Console test of the service layer and the APPROVAL TRANSACTION.
 *
 * !! This test CHANGES the database. Run it on fresh sample data and reload
 *    the sample data afterwards:
 *      mysql -u campusride -p < sql/01_schema.sql
 *      mysql -u campusride -p < sql/02_sample_data.sql
 *
 * Run:  mvn -q test-compile exec:java -Dexec.mainClass=com.campusride.ServiceTest -Dexec.classpathScope=test
 */
public class ServiceTest {

    private static int passed = 0;
    private static int failed = 0;

    private static final AuthService auth = new AuthService();
    private static final AdminService admin = new AdminService();
    private static final ApplicationService apps = new ApplicationService();

    public static void main(String[] args) throws Exception {
        System.out.println("--- login ---");
        expectError("wrong password refused", "Incorrect", () -> auth.login("admin", "nope1234".toCharArray()));
        auth.login("admin", "Admin@2026".toCharArray());
        check("admin logs in", true);

        System.out.println("--- approval AT capacity (R3 is 2/2 full) ---");
        expectError("approve Karan's app 5 on full R3 fails", "full", () -> admin.approve(5, null));
        check("app 5 still SUBMITTED after rollback", "SUBMITTED".equals(status(5)));
        check("no pass was created for app 5", countPasses() == 2);

        System.out.println("--- approval BELOW capacity (R1 has 40 seats) ---");
        String passNo = admin.approve(4, "OK");
        check("approve Arjun's app 4 -> pass " + passNo, "CR-2026-00003".equals(passNo));
        check("app 4 now APPROVED", "APPROVED".equals(status(4)));
        check("passenger list R1 now has 1 passenger", admin.getPassengers(1).size() == 1);
        expectError("approving app 4 twice is refused", "already APPROVED", () -> admin.approve(4, null));

        System.out.println("--- registration validation ---");
        expectError("bad phone refused", "10 digits", () -> register("newbie1", "Passw0rd!", "TEST001", "12345"));
        expectError("weak password refused", "letter and one digit", () -> register("newbie1", "password", "TEST001", "9876543210"));
        expectError("duplicate username refused", "already taken", () -> register("riya", "Passw0rd!", "TEST001", "9876543210"));
        expectError("duplicate roll no refused", "roll number", () -> register("newbie1", "Passw0rd!", "RA2411003010101", "9876543210"));
        register("newbie1", "Passw0rd!", "TEST001", "9876543210");
        check("valid registration works", true);

        System.out.println("--- student rules ---");
        auth.login("riya", "Student@123".toCharArray());
        LocalDate from = LocalDate.now().plusDays(1);
        LocalDate to = from.plusDays(60);
        expectError("Riya cannot apply while holding an overlapping ACTIVE pass", "ACTIVE pass",
                () -> apps.createApplication(1, from, to, null, true));
        expectError("valid_to before valid_from refused", "after",
                () -> apps.createApplication(1, to, from, null, false));
        expectError("stop must be selected", "select a stop",
                () -> apps.createApplication(null, from, to, null, false));

        auth.login("newbie1", "Passw0rd!".toCharArray());
        expectError("cannot SUBMIT to full R3", "full",
                () -> apps.createApplication(10, from, to, null, true));
        int draftId = apps.createApplication(10, from, to, "draft is fine", false);
        check("can save a DRAFT for R3", draftId > 0);

        System.out.println("--- free one R3 seat, then race two approvals for it ---");
        auth.login("admin", "Admin@2026".toCharArray());
        admin.cancelPass(1);   // Riya's R3 pass -> CANCELLED, R3 now 1/2
        check("after cancel R3 has 1 passenger", admin.getPassengers(3).size() == 1);

        auth.login("newbie1", "Passw0rd!".toCharArray());
        apps.submitDraft(draftId);
        check("newbie1's draft submitted (1 seat left)", "SUBMITTED".equals(status(draftId)));

        auth.login("admin", "Admin@2026".toCharArray());
        // Two threads = two admins pressing Approve at the same moment for the LAST seat
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger full = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        int[] ids = {5, draftId};
        Thread[] threads = new Thread[2];
        for (int i = 0; i < 2; i++) {
            final int appId = ids[i];
            threads[i] = new Thread(() -> {
                try {
                    start.await();
                    admin.approve(appId, null);
                    ok.incrementAndGet();
                } catch (ServiceException e) {
                    if (e.getMessage().contains("full")) full.incrementAndGet();
                    System.out.println("        (app " + appId + " refused: " + e.getMessage() + ")");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            threads[i].start();
        }
        start.countDown();
        for (Thread t : threads) t.join();
        check("exactly ONE of the two racing approvals succeeded", ok.get() == 1 && full.get() == 1);
        check("R3 has exactly 2 passengers (never over capacity)", admin.getPassengers(3).size() == 2);

        System.out.println("--- reject / delete rules ---");
        expectError("reject needs a reason", "required", () -> admin.reject(999, " "));
        auth.login("karan", "Student@123".toCharArray());
        apps.deleteDraft(6);
        check("Karan deleted his DRAFT app 6", status(6) == null);
        expectError("Karan cannot delete SUBMITTED/APPROVED app 5", "DRAFT", () -> apps.deleteDraft(5));

        System.out.println();
        System.out.println("Service test: " + passed + " passed, " + failed + " failed");
        System.out.println("Remember to reload the sample data (sql/01 and sql/02).");
        if (failed > 0) System.exit(1);
    }

    // ------------------------------------------------------------------ helpers

    interface Action { void run() throws Exception; }

    private static void expectError(String name, String expectedText, Action action) {
        try {
            action.run();
            check(name + "  (no error was raised!)", false);
        } catch (ServiceException e) {
            boolean ok = e.getMessage().contains(expectedText);
            check(name + "  -> \"" + e.getMessage() + "\"", ok);
        } catch (Exception e) {
            check(name + "  (unexpected " + e + ")", false);
        }
    }

    private static void register(String user, String pwd, String roll, String phone) throws ServiceException {
        Student s = new Student();
        s.setRollNo(roll);
        s.setFullName("Test Student");
        s.setDepartment("CSE");
        s.setYearOfStudy(1);
        s.setPhone(phone);
        s.setEmail("test@example.com");
        auth.registerStudent(user, pwd.toCharArray(), pwd.toCharArray(), s);
    }

    private static String status(int applicationId) throws Exception {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT status FROM pass_applications WHERE application_id = ?")) {
            ps.setInt(1, applicationId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private static int countPasses() throws Exception {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM bus_passes");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "  PASS  " : "  FAIL  ") + name);
        if (ok) passed++; else failed++;
    }
}
