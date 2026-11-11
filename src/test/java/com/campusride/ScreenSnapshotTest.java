package com.campusride;

import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import com.campusride.service.AuthService;
import com.campusride.ui.AdminDashboard;
import com.campusride.ui.LoginFrame;
import com.campusride.ui.StudentDashboard;
import com.campusride.ui.UiUtil;

/**
 * Builds every screen against the sample database (off-screen) and
 * saves a PNG of each tab into target/screenshots/. Proves that every screen
 * opens without errors and lets us check the layout. Read-only: it changes no data.
 *
 * Run:  mvn -q test-compile exec:java -Dexec.mainClass=com.campusride.ScreenSnapshotTest -Dexec.classpathScope=test
 */
public class ScreenSnapshotTest {

    private static final File OUT = new File("target/screenshots");

    public static void main(String[] args) throws Exception {
        OUT.mkdirs();
        SwingUtilities.invokeAndWait(() -> {
            try {
                UiUtil.applyTheme();
                AuthService auth = new AuthService();

                LoginFrame login = new LoginFrame();
                snapshotAllTabs(login, "1_login");
                login.dispose();

                auth.login("riya", "Student@123".toCharArray());
                StudentDashboard student = new StudentDashboard();
                // select the first route and its second stop on the Routes tab
                List<JTable> tables = findAll(student, JTable.class);
                tables.get(0).setRowSelectionInterval(2, 2);   // R3 (full) - shows red "0 of 2"
                tables.get(1).setRowSelectionInterval(1, 1);
                snapshotAllTabs(student, "2_student");
                student.dispose();

                auth.login("admin", "Admin@2026".toCharArray());
                AdminDashboard admin = new AdminDashboard();
                List<JTable> adminTables = findAll(admin, JTable.class);
                adminTables.get(0).setRowSelectionInterval(0, 0);   // first pending application
                snapshotAllTabs(admin, "3_admin");
                // second pending row (Karan, full route) for the red capacity panel
                findAll(admin, JTabbedPane.class).get(0).setSelectedIndex(0);
                adminTables.get(0).setRowSelectionInterval(1, 1);
                save(admin, "3_admin_tab0_full_route");
                admin.dispose();
                System.out.println("Screenshots written to " + OUT.getAbsolutePath());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        System.exit(0);
    }

    /** Saves one PNG per tab of the window's first JTabbedPane. */
    private static void snapshotAllTabs(JFrame frame, String name) throws Exception {
        frame.setLocation(-4000, 0);   // shown, but off-screen, so text is measured exactly as in real use
        frame.setVisible(true);
        frame.validate();
        JTabbedPane tabs = findAll(frame, JTabbedPane.class).get(0);
        for (int i = 0; i < tabs.getTabCount(); i++) {
            tabs.setSelectedIndex(i);
            save(frame, name + "_tab" + i + "_" + tabs.getTitleAt(i).replaceAll("[^A-Za-z]+", "_"));
        }
    }

    private static void save(JFrame frame, String name) throws Exception {
        frame.validate();
        Container root = frame.getRootPane();
        // Paint at the monitor's scale (e.g. 1.25 for 125% Windows scaling), like the real window
        double scale = frame.getGraphicsConfiguration().getDefaultTransform().getScaleX();
        BufferedImage img = new BufferedImage((int) (root.getWidth() * scale), (int) (root.getHeight() * scale),
                BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.scale(scale, scale);
        root.printAll(g);
        g.dispose();
        ImageIO.write(img, "png", new File(OUT, name + ".png"));
        System.out.println("  saved " + name + ".png");
    }

    /** Finds all components of a type inside a container, depth-first in layout order. */
    private static <T> List<T> findAll(Container c, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Component child : c.getComponents()) {
            if (type.isInstance(child)) {
                found.add(type.cast(child));
            }
            if (child instanceof Container) {
                found.addAll(findAll((Container) child, type));
            }
        }
        return found;
    }
}
