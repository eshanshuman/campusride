package com.campusride.service;

import java.sql.Connection;
import java.sql.SQLException;

import com.campusride.dao.StudentDAO;
import com.campusride.model.Student;
import com.campusride.util.DBConnection;
import com.campusride.util.Session;

// profile of the logged-in student
public class StudentService {

    private StudentDAO studentDAO = new StudentDAO();

    public Student getMyProfile() throws ServiceException {
        try (Connection con = DBConnection.getConnection()) {
            Student s = studentDAO.findById(con, currentStudentId());
            Session.setStudent(s);
            return s;
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    // roll number can't be changed, so it is not a parameter here
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

    // student_id of whoever is logged in (ApplicationService uses this too)
    static int currentStudentId() throws ServiceException {
        if (Session.getStudent() == null) {
            throw new ServiceException("Please log in as a student first.");
        }
        return Session.getStudent().getStudentId();
    }
}
