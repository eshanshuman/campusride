package com.campusride.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.campusride.model.Student;
import com.campusride.service.ServiceException;
import com.campusride.service.StudentService;
import com.campusride.util.Session;

// "My Profile" tab: the student can see and edit their details.
// Username and roll number are shown but can't be changed.
public class ProfilePanel extends JPanel {

    private StudentService studentService = new StudentService();
    private StudentDashboard dashboard;

    private JTextField username = new JTextField(22);
    private JTextField rollNo = new JTextField(22);
    private JTextField fullName = new JTextField(22);
    private JComboBox<String> department = new JComboBox<>(LoginFrame.DEPARTMENTS);
    private JComboBox<Integer> year = new JComboBox<>(LoginFrame.YEARS);
    private JTextField phone = new JTextField(22);
    private JTextField email = new JTextField(22);

    public ProfilePanel(StudentDashboard dashboard) {
        this.dashboard = dashboard;
        setLayout(new BorderLayout());
        setBackground(UiUtil.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        username.setEditable(false);
        rollNo.setEditable(false);

        JPanel form = UiUtil.section("My details");
        form.setLayout(new GridBagLayout());
        UiUtil.addRow(form, 0, "Username", username);
        UiUtil.addRow(form, 1, "Roll number", rollNo);
        UiUtil.addRow(form, 2, "Full name *", fullName);
        UiUtil.addRow(form, 3, "Department *", department);
        UiUtil.addRow(form, 4, "Year of study *", year);
        UiUtil.addRow(form, 5, "Phone (10 digits) *", phone);
        UiUtil.addRow(form, 6, "Email *", email);

        JButton save = UiUtil.primaryButton("Save changes");
        save.addActionListener(e -> save());
        JButton undo = UiUtil.secondaryButton("Undo changes");
        undo.addActionListener(e -> loadProfile());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setOpaque(false);
        buttons.add(undo);
        buttons.add(save);
        UiUtil.addWide(form, 7, buttons);

        // keep the form small in the top-left corner
        JPanel holder = new JPanel(new FlowLayout(FlowLayout.LEFT));
        holder.setOpaque(false);
        holder.add(form);
        add(holder, BorderLayout.NORTH);

        loadProfile();
    }

    // fill the fields from the database
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
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void save() {
        try {
            studentService.updateMyProfile(fullName.getText(), (String) department.getSelectedItem(),
                    (Integer) year.getSelectedItem(), phone.getText().trim(), email.getText());
            JOptionPane.showMessageDialog(this, "Profile saved.");
            dashboard.refreshWelcome();   // the name in the header may have changed
        } catch (ServiceException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
