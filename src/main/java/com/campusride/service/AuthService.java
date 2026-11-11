package com.campusride.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

import com.campusride.dao.StudentDAO;
import com.campusride.dao.UserDAO;
import com.campusride.model.Student;
import com.campusride.model.User;
import com.campusride.util.DBConnection;
import com.campusride.util.PasswordUtil;
import com.campusride.util.Session;

/** Login, registration and logout. */
public class AuthService {

    private final UserDAO userDAO = new UserDAO();
    private final StudentDAO studentDAO = new StudentDAO();

    /**
     * Checks username + password. On success stores the user in Session and
     * returns it. We give the SAME message for "no such user" and "wrong
     * password", so nobody can find out which usernames exist.
     */
    public User login(String username, char[] password) throws ServiceException {
        if (username == null || username.trim().isEmpty() || password == null || password.length == 0) {
            throw new ServiceException("Please enter your username and password.");
        }
        try (Connection con = DBConnection.getConnection()) {
            User user = userDAO.findByUsername(con, username.trim());
            if (user == null || !PasswordUtil.verify(password, user.getPasswordSalt(), user.getPasswordHash())) {
                throw new ServiceException("Incorrect username or password.");
            }
            Student student = null;
            if (!user.isAdmin()) {
                student = studentDAO.findByUserId(con, user.getUserId());
            }
            Session.login(user, student);
            return user;
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        } finally {
            Arrays.fill(password, ' ');   // wipe the password from memory
        }
    }

    /**
     * Registers a new STUDENT. Two INSERTs (users, then students) run in ONE
     * transaction: if the second fails, the first is rolled back, so we never
     * get a login without a student profile.
     */
    public void registerStudent(String username, char[] password, char[] confirm, Student s)
            throws ServiceException {
        // ---- 1. validate every field (friendly messages) ----
        String uname = Validation.username(username);
        Validation.password(password);
        if (!Arrays.equals(password, confirm)) {
            throw new ServiceException("The two passwords do not match.");
        }
        s.setRollNo(Validation.required(s.getRollNo(), "Roll number", 20).toUpperCase());
        s.setFullName(Validation.required(s.getFullName(), "Full name", 80));
        s.setDepartment(Validation.required(s.getDepartment(), "Department", 50));
        Validation.yearOfStudy(s.getYearOfStudy());
        s.setPhone(Validation.phone(s.getPhone()));
        s.setEmail(Validation.email(s.getEmail()));

        // ---- 2. save in one transaction ----
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                // Friendly uniqueness checks first (the UNIQUE keys are the final guard)
                if (userDAO.usernameExists(con, uname)) {
                    throw new ServiceException("That username is already taken. Please choose another.");
                }
                if (studentDAO.rollNoExists(con, s.getRollNo(), 0)) {
                    throw new ServiceException("That roll number is already registered.");
                }
                User u = new User();
                u.setUsername(uname);
                u.setRole("STUDENT");
                u.setPasswordSalt(PasswordUtil.newSalt());
                u.setPasswordHash(PasswordUtil.hash(password, u.getPasswordSalt()));
                int userId = userDAO.insert(con, u);

                s.setUserId(userId);
                studentDAO.insert(con, s);
                con.commit();
            } catch (ServiceException | SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        } finally {
            Arrays.fill(password, ' ');
            if (confirm != null) Arrays.fill(confirm, ' ');
        }
    }

    public void logout() {
        Session.logout();
    }
}
