package com.campusride.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import com.campusride.model.Student;
import com.campusride.model.User;
import com.campusride.service.AuthService;
import com.campusride.service.ServiceException;

// First screen: a Login tab and a Register tab (registering is only for students).
public class LoginFrame extends JFrame implements ActionListener {

    // also used by ProfilePanel
    static final String[] DEPARTMENTS = {"CSE", "IT", "ECE", "EEE", "MECH", "CIVIL", "BIOTECH", "OTHER"};
    static final Integer[] YEARS = {1, 2, 3, 4, 5};

    private AuthService authService = new AuthService();
    private JTabbedPane tabs = new JTabbedPane();

    // login tab
    private JTextField loginUser = new JTextField(18);
    private JPasswordField loginPass = new JPasswordField(18);
    private JButton loginBtn = UiUtil.primaryButton("Log in");

    // register tab
    private JTextField regUser = new JTextField(18);
    private JPasswordField regPass = new JPasswordField(18);
    private JPasswordField regConfirm = new JPasswordField(18);
    private JTextField regRoll = new JTextField(18);
    private JTextField regName = new JTextField(18);
    private JComboBox<String> regDept = new JComboBox<String>(DEPARTMENTS);
    private JComboBox<Integer> regYear = new JComboBox<Integer>(YEARS);
    private JTextField regPhone = new JTextField(18);
    private JTextField regEmail = new JTextField(18);
    private JButton registerBtn = UiUtil.primaryButton("Create account");

    public LoginFrame() {
        super("CampusRide - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(UiUtil.header("CampusRide", "College Bus Pass & Route Allocation System"), BorderLayout.NORTH);

        tabs.addTab("Login", buildLoginTab());
        tabs.addTab("Register (students)", buildRegisterTab());
        JPanel centre = new JPanel(new BorderLayout());
        centre.setBackground(UiUtil.BACKGROUND);
        centre.setBorder(BorderFactory.createEmptyBorder(14, 18, 18, 18));
        centre.add(tabs, BorderLayout.CENTER);
        add(centre, BorderLayout.CENTER);

        // this class handles all the button clicks (see actionPerformed below)
        loginBtn.addActionListener(this);
        loginPass.addActionListener(this);   // pressing Enter in the password box also logs in
        registerBtn.addActionListener(this);

        setSize(480, 600);
        setLocationRelativeTo(null);   // middle of the screen
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == loginBtn || e.getSource() == loginPass) {
            doLogin();
        } else if (e.getSource() == registerBtn) {
            doRegister();
        }
    }

    private JPanel buildLoginTab() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(30, 20, 20, 20));
        UiUtil.addRow(form, 0, "Username", loginUser);
        UiUtil.addRow(form, 1, "Password", loginPass);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setOpaque(false);
        buttons.add(loginBtn);
        UiUtil.addWide(form, 2, buttons);

        // put the form at the top so it doesn't stretch over the whole tab
        JPanel tab = new JPanel(new BorderLayout());
        tab.setBackground(Color.WHITE);
        tab.add(form, BorderLayout.NORTH);
        return tab;
    }

    private JPanel buildRegisterTab() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        UiUtil.addRow(form, 0, "Username *", regUser);
        UiUtil.addRow(form, 1, "Password *", regPass);
        UiUtil.addRow(form, 2, "Confirm password *", regConfirm);
        UiUtil.addRow(form, 3, "Roll number *", regRoll);
        UiUtil.addRow(form, 4, "Full name *", regName);
        UiUtil.addRow(form, 5, "Department *", regDept);
        UiUtil.addRow(form, 6, "Year of study *", regYear);
        UiUtil.addRow(form, 7, "Phone (10 digits) *", regPhone);
        UiUtil.addRow(form, 8, "Email *", regEmail);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setOpaque(false);
        buttons.add(registerBtn);
        UiUtil.addWide(form, 9, buttons);

        JPanel tab = new JPanel(new BorderLayout());
        tab.setBackground(Color.WHITE);
        tab.add(form, BorderLayout.NORTH);
        return tab;
    }

    private void doLogin() {
        try {
            User user = authService.login(loginUser.getText(), loginPass.getPassword());
            loginPass.setText("");
            if (user.isAdmin()) {
                new AdminDashboard().setVisible(true);
            } else {
                new StudentDashboard().setVisible(true);
            }
            dispose();   // close the login window
        } catch (ServiceException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doRegister() {
        Student s = new Student();
        s.setRollNo(regRoll.getText());
        s.setFullName(regName.getText());
        s.setDepartment((String) regDept.getSelectedItem());
        s.setYearOfStudy((Integer) regYear.getSelectedItem());
        s.setPhone(regPhone.getText().trim());
        s.setEmail(regEmail.getText());
        try {
            authService.registerStudent(regUser.getText(), regPass.getPassword(), regConfirm.getPassword(), s);
            String newUsername = regUser.getText().trim();
            JOptionPane.showMessageDialog(this, "Account created! You can now log in as '" + newUsername + "'.");

            // go to the login tab with the username already filled in
            loginUser.setText(newUsername);
            clearRegisterForm();
            tabs.setSelectedIndex(0);
            loginPass.requestFocusInWindow();
        } catch (ServiceException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearRegisterForm() {
        regUser.setText("");
        regPass.setText("");
        regConfirm.setText("");
        regRoll.setText("");
        regName.setText("");
        regPhone.setText("");
        regEmail.setText("");
        regDept.setSelectedIndex(0);
        regYear.setSelectedIndex(0);
    }
}
