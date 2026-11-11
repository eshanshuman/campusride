package com.campusride.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.campusride.model.Student;
import com.campusride.service.ServiceException;
import com.campusride.service.StudentService;
import com.campusride.util.Session;

/**
 * Screen: "My Profile" - view and edit the student's details (UPDATE students).
 * Username and roll number are shown but cannot be changed.
 */
public class ProfilePanel extends JPanel {

    private final StudentService studentService = new StudentService();
    private final StudentDashboard dashboard;

    private final JTextField username = new JTextField(22);
    private final JTextField rollNo = new JTextField(22);
    private final JTextField fullName = new JTextField(22);
    private final JComboBox<String> department = new JComboBox<>(LoginFrame.DEPARTMENTS);
    private final JComboBox<Integer> year = new JComboBox<>(LoginFrame.YEARS);
    private final JTextField phone = new JTextField(22);
    private final JTextField email = new JTextField(22);

    public ProfilePanel(StudentDashboard dashboard) {
        this.dashboard = dashboard;
        setLayout(new BorderLayout());
        setBackground(UiUtil.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        username.setEditable(false);
        rollNo.setEditable(false);

        JPanel form = UiUtil.section("My details");
        form.setLayout(new GridBagLayout());
        int row = 0;
        UiUtil.addRow(form, row++, "Username", username);
        UiUtil.addRow(form, row++, "Roll number", rollNo);
        UiUtil.addRow(form, row++, "Full name *", fullName);
        UiUtil.addRow(form, row++, "Department *", department);
        UiUtil.addRow(form, row++, "Year of study *", year);
        UiUtil.addRow(form, row++, "Phone (10 digits) *", phone);
        UiUtil.addRow(form, row++, "Email *", email);

        JButton save = UiUtil.primaryButton("Save changes");
        save.addActionListener(e -> save());
        JButton reset = UiUtil.secondaryButton("Undo changes");
        reset.addActionListener(e -> loadProfile());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setOpaque(false);
        buttons.add(reset);
        buttons.add(save);
        UiUtil.addWide(form, row, buttons);

        // Keep the form at a sensible width at the top-left
        JPanel holder = new JPanel(new FlowLayout(FlowLayout.LEFT));
        holder.setOpaque(false);
        holder.add(form);
        add(holder, BorderLayout.NORTH);

        loadProfile();
    }

    /** Fills the fields from the database. */
    public void loadProfile() {
        try {
            Student s = studentService.getMyProfile();
            username.setText(Session.getUser().getUsername());
            rollNo.setText(s.getRollNo());
            fullName.setText(s.getFullName());
            department.setSelectedItem(s.getDepartment());
            year.setSelectedItem(s.getYearOfStudy());
            phone.setText(s.getPhone());
            email.setText(s.getEmail());
        } catch (ServiceException ex) {
            UiUtil.error(this, ex.getMessage());
        }
    }

    private void save() {
        try {
            studentService.updateMyProfile(fullName.getText(), (String) department.getSelectedItem(),
                    (Integer) year.getSelectedItem(), phone.getText().trim(), email.getText());
            UiUtil.info(this, "Profile saved.");
            dashboard.refreshWelcome();
        } catch (ServiceException ex) {
            UiUtil.error(this, ex.getMessage());
        }
    }
}
