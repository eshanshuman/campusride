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

import com.campusride.model.Route;
import com.campusride.model.Stop;
import com.campusride.service.AuthService;
import com.campusride.util.Session;

// Main window for a student: header with Logout, and 4 tabs.
public class StudentDashboard extends JFrame {

    private JTabbedPane tabs = new JTabbedPane();
    private JLabel welcome = new JLabel();
    private ApplicationPanel applicationPanel;
    private PassViewPanel passViewPanel;

    public StudentDashboard() {
        super("CampusRide - Student");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // header: title on the left, "Welcome, name" and Logout on the right
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

        applicationPanel = new ApplicationPanel();
        passViewPanel = new PassViewPanel();
        tabs.addTab("Routes & Timings", new RoutesPanel(this));
        tabs.addTab("Apply for Pass", applicationPanel);
        tabs.addTab("My Bus Pass", passViewPanel);
        tabs.addTab("My Profile", new ProfilePanel(this));

        // reload the data each time these tabs are opened, so the status is always up to date
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

    // RoutesPanel calls this when the student clicks "Apply for this stop"
    public void openApplicationFor(Route route, Stop stop, LocalDate from, LocalDate to) {
        applicationPanel.prefill(route, stop, from, to);
        tabs.setSelectedComponent(applicationPanel);
    }

    // after login or after editing the profile
    public void refreshWelcome() {
        String name = "";
        if (Session.getStudent() != null) {
            name = Session.getStudent().getFullName();
        }
        welcome.setText("Welcome, " + name);
    }

    private void logout() {
        new AuthService().logout();
        dispose();
        new LoginFrame().setVisible(true);
    }
}
