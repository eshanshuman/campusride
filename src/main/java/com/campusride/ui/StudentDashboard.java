package com.campusride.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.time.LocalDate;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

import com.campusride.model.Route;
import com.campusride.model.Stop;
import com.campusride.service.AuthService;
import com.campusride.util.Session;

/**
 * Screen: the student's main window. A header with a Logout button and four
 * tabs: Routes & Timings, Apply for Pass, My Bus Pass, My Profile.
 */
public class StudentDashboard extends JFrame {

    private static final int TAB_APPLY = 1;

    private final JTabbedPane tabs = new JTabbedPane();
    private final JLabel welcome = new JLabel();
    private final ApplicationPanel applicationPanel;
    private final PassViewPanel passViewPanel;

    public StudentDashboard() {
        super("CampusRide - Student");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // ---- header with welcome text and logout ----
        JPanel header = UiUtil.header("CampusRide", "Student portal - routes, applications and your bus pass");
        welcome.setForeground(Color.WHITE);
        JButton logout = UiUtil.secondaryButton("Logout");
        logout.addActionListener(e -> logout());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 6));
        right.setOpaque(false);
        right.add(welcome);
        right.add(logout);
        header.add(right, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        refreshWelcome();

        // ---- tabs ----
        applicationPanel = new ApplicationPanel(this);
        passViewPanel = new PassViewPanel();
        tabs.addTab("Routes & Timings", new RoutesPanel(this));
        tabs.addTab("Apply for Pass", applicationPanel);
        tabs.addTab("My Bus Pass", passViewPanel);
        tabs.addTab("My Profile", new ProfilePanel(this));
        // Refresh status/pass data every time the student opens those tabs
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedComponent() == applicationPanel) {
                applicationPanel.loadMyApplications();
            } else if (tabs.getSelectedComponent() == passViewPanel) {
                passViewPanel.loadPasses();
            }
        });
        add(tabs, BorderLayout.CENTER);

        setSize(1050, 700);
        setLocationRelativeTo(null);
    }

    /** RoutesPanel calls this: switch to the Apply tab with the form pre-filled. */
    public void openApplicationFor(Route route, Stop stop, LocalDate from, LocalDate to) {
        applicationPanel.prefill(route, stop, from, to);
        tabs.setSelectedIndex(TAB_APPLY);
    }

    /** Updates "Welcome, name" (after login or a profile edit). */
    public void refreshWelcome() {
        String name = Session.getStudent() == null ? "" : Session.getStudent().getFullName();
        welcome.setText("Welcome, " + name);
    }

    private void logout() {
        new AuthService().logout();
        dispose();
        new LoginFrame().setVisible(true);
    }
}
