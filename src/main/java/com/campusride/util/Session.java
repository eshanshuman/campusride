package com.campusride.util;

import com.campusride.model.Student;
import com.campusride.model.User;

// Remembers who is logged in. It's a desktop app, so only one user at a time.
public class Session {

    private static User currentUser;
    private static Student currentStudent;   // stays null when the admin logs in

    public static void login(User user, Student student) {
        currentUser = user;
        currentStudent = student;
    }

    public static void logout() {
        currentUser = null;
        currentStudent = null;
    }

    public static User getUser() {
        return currentUser;
    }

    public static Student getStudent() {
        return currentStudent;
    }

    // called after the student edits their profile
    public static void setStudent(Student student) {
        currentStudent = student;
    }
}
