package com.campusride.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.campusride.model.Student;

/** SQL for the students table. */
public class StudentDAO {

    private static final String COLUMNS =
        "student_id, user_id, roll_no, full_name, department, year_of_study, phone, email";

    public Student findByUserId(Connection con, int userId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM students WHERE user_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public Student findById(Connection con, int studentId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM students WHERE student_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /**
     * Locks the student's row until the current transaction ends (FOR UPDATE).
     * Used during approval so two admins cannot give the same student two
     * overlapping passes at the same moment. Returns false if not found.
     */
    public boolean lockById(Connection con, int studentId) throws SQLException {
        String sql = "SELECT student_id FROM students WHERE student_id = ? FOR UPDATE";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** True if a student other than excludeStudentId already uses this roll number. */
    public boolean rollNoExists(Connection con, String rollNo, int excludeStudentId) throws SQLException {
        String sql = "SELECT 1 FROM students WHERE roll_no = ? AND student_id <> ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, rollNo);
            ps.setInt(2, excludeStudentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** INSERT used by registration; returns the new student_id. */
    public int insert(Connection con, Student s) throws SQLException {
        String sql = "INSERT INTO students (user_id, roll_no, full_name, department, year_of_study, phone, email) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, s.getUserId());
            ps.setString(2, s.getRollNo());
            ps.setString(3, s.getFullName());
            ps.setString(4, s.getDepartment());
            ps.setInt(5, s.getYearOfStudy());
            ps.setString(6, s.getPhone());
            ps.setString(7, s.getEmail());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    /** UPDATE used by "Edit profile". The roll number cannot be changed after registration. */
    public int update(Connection con, Student s) throws SQLException {
        String sql = "UPDATE students SET full_name = ?, department = ?, year_of_study = ?, phone = ?, email = ? "
                   + "WHERE student_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, s.getFullName());
            ps.setString(2, s.getDepartment());
            ps.setInt(3, s.getYearOfStudy());
            ps.setString(4, s.getPhone());
            ps.setString(5, s.getEmail());
            ps.setInt(6, s.getStudentId());
            return ps.executeUpdate();
        }
    }

    private Student map(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setStudentId(rs.getInt("student_id"));
        s.setUserId(rs.getInt("user_id"));
        s.setRollNo(rs.getString("roll_no"));
        s.setFullName(rs.getString("full_name"));
        s.setDepartment(rs.getString("department"));
        s.setYearOfStudy(rs.getInt("year_of_study"));
        s.setPhone(rs.getString("phone"));
        s.setEmail(rs.getString("email"));
        return s;
    }
}
