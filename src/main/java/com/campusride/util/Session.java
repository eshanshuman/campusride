package com.campusride.util;

import com.campusride.model.Student;
import com.campusride.model.User;

/**
 * Remembers who is logged in while the app is running.
 * It is a simple "static holder": there is only one user per desktop app.
 */
public final class Session {

    private static User currentUser;        // set at login
    private static Student currentStudent;  // set at login if role = STUDENT, else null

    private Session() { }

    public static void login(User user, Student student) {
        currentUser = user;
        currentStudent = student;
    }

    public static void logout() {
        currentUser = null;
        currentStudent = null;
    }

    public static User getUser() { return currentUser; }

    public static Student getStudent() { return currentStudent; }

    /** Used after the student edits their profile. */
    public static void setStudent(Student student) { currentStudent = student; }

    public static boolean isLoggedIn() { return currentUser != null; }
}
