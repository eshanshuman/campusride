package com.campusride.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import com.campusride.model.Student;
import com.campusride.model.User;
import com.campusride.service.AuthService;
import com.campusride.service.ServiceException;

/**
 * Screen 1: Login and Register (two tabs).
 * After a successful login it opens StudentDashboard or AdminDashboard.
 */
public class LoginFrame extends JFrame {

    /** Department choices, shared with ProfilePanel. */
    static final String[] DEPARTMENTS = {"CSE", "IT", "ECE", "EEE", "MECH", "CIVIL", "BIOTECH", "OTHER"};
    static final Integer[] YEARS = {1, 2, 3, 4, 5};

    private final AuthService authService = new AuthService();

    // ---- Login tab ----
    private final JTextField loginUser = new JTextField(18);
    private final JPasswordField loginPass = new JPasswordField(18);

    // ---- Register tab ----
    private final JTextField regUser = new JTextField(18);
    private final JPasswordField regPass = new JPasswordField(18);
    private final JPasswordField regConfirm = new JPasswordField(18);
    private final JTextField regRoll = new JTextField(18);
    private final JTextField regName = new JTextField(18);
    private final JComboBox<String> regDept = new JComboBox<>(DEPARTMENTS);
    private final JComboBox<Integer> regYear = new JComboBox<>(YEARS);
    private final JTextField regPhone = new JTextField(18);
    private final JTextField regEmail = new JTextField(18);

    private final JTabbedPane tabs = new JTabbedPane();

    public LoginFrame() {
        super("CampusRide - Login");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UiUtil.BACKGROUND);

        add(UiUtil.header("CampusRide", "College Bus Pass & Route Allocation System"), BorderLayout.NORTH);

        tabs.addTab("Login", buildLoginTab());
        tabs.addTab("Register (students)", buildRegisterTab());
        JPanel centre = new JPanel(new BorderLayout());
        centre.setBackground(UiUtil.BACKGROUND);
        centre.setBorder(BorderFactory.createEmptyBorder(14, 18, 18, 18));
        centre.add(tabs, BorderLayout.CENTER);
        add(centre, BorderLayout.CENTER);

        setSize(480, 600);
        setLocationRelativeTo(null);   // centre on screen
    }

    private JPanel buildLoginTab() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(30, 20, 20, 20));
        UiUtil.addRow(form, 0, "Username", loginUser);
        UiUtil.addRow(form, 1, "Password", loginPass);

        JButton loginBtn = UiUtil.primaryButton("Log in");
        loginBtn.addActionListener(e -> doLogin());
        loginPass.addActionListener(e -> doLogin());   // Enter key in password field

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setOpaque(false);
        buttons.add(loginBtn);
        UiUtil.addWide(form, 2, buttons);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.add(form, BorderLayout.NORTH);
        return wrapper;
    }

    private JPanel buildRegisterTab() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        int row = 0;
        UiUtil.addRow(form, row++, "Username *", regUser);
        UiUtil.addRow(form, row++, "Password *", regPass);
        UiUtil.addRow(form, row++, "Confirm password *", regConfirm);
        UiUtil.addRow(form, row++, "Roll number *", regRoll);
        UiUtil.addRow(form, row++, "Full name *", regName);
        UiUtil.addRow(form, row++, "Department *", regDept);
        UiUtil.addRow(form, row++, "Year of study *", regYear);
        UiUtil.addRow(form, row++, "Phone (10 digits) *", regPhone);
        UiUtil.addRow(form, row++, "Email *", regEmail);

        JButton registerBtn = UiUtil.primaryButton("Create account");
        registerBtn.addActionListener(e -> doRegister());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setOpaque(false);
        buttons.add(registerBtn);
        UiUtil.addWide(form, row, buttons);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.add(form, BorderLayout.NORTH);
        return wrapper;
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
            UiUtil.error(this, ex.getMessage());
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
            UiUtil.info(this, "Account created! You can now log in as '" + regUser.getText().trim() + "'.");
            loginUser.setText(regUser.getText().trim());
            clearRegisterForm();
            tabs.setSelectedIndex(0);
            loginPass.requestFocusInWindow();
        } catch (ServiceException ex) {
            UiUtil.error(this, ex.getMessage());
        }
    }

    private void clearRegisterForm() {
        for (JTextField f : new JTextField[] {regUser, regPass, regConfirm, regRoll, regName, regPhone, regEmail}) {
            f.setText("");
        }
        regDept.setSelectedIndex(0);
        regYear.setSelectedIndex(0);
    }
}
