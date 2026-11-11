package com.campusride.service;

import java.sql.Connection;
import java.sql.SQLException;

import com.campusride.dao.StudentDAO;
import com.campusride.model.Student;
import com.campusride.util.DBConnection;
import com.campusride.util.Session;

/** Profile of the logged-in student. */
public class StudentService {

    private final StudentDAO studentDAO = new StudentDAO();

    /** Re-reads the logged-in student's profile from the database. */
    public Student getMyProfile() throws ServiceException {
        try (Connection con = DBConnection.getConnection()) {
            Student s = studentDAO.findById(con, currentStudentId());
            Session.setStudent(s);
            return s;
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    /** Validates and saves the edited profile (roll number cannot change). */
    public void updateMyProfile(String fullName, String department, int year, String phone, String email)
            throws ServiceException {
        Student s = new Student();
        s.setStudentId(currentStudentId());
        s.setFullName(Validation.required(fullName, "Full name", 80));
        s.setDepartment(Validation.required(department, "Department", 50));
        Validation.yearOfStudy(year);
        s.setYearOfStudy(year);
        s.setPhone(Validation.phone(phone));
        s.setEmail(Validation.email(email));

        try (Connection con = DBConnection.getConnection()) {
            studentDAO.update(con, s);
            Session.setStudent(studentDAO.findById(con, s.getStudentId()));
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    /** student_id of whoever is logged in; refuses if it is not a student. */
    static int currentStudentId() throws ServiceException {
        if (Session.getStudent() == null) {
            throw new ServiceException("Please log in as a student first.");
        }
        return Session.getStudent().getStudentId();
    }
}
