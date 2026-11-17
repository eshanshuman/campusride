package com.campusride.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.campusride.model.Student;

// SQL for the students table (same try/finally pattern as UserDAO)
public class StudentDAO {

    public Student findByUserId(Connection con, int userId) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement("SELECT * FROM students WHERE user_id = ?");
            ps.setInt(1, userId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return readStudent(rs);
            }
            return null;
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
    }

    public Student findById(Connection con, int studentId) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement("SELECT * FROM students WHERE student_id = ?");
            ps.setInt(1, studentId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return readStudent(rs);
            }
            return null;
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
    }

    // FOR UPDATE locks this student's row until we commit or rollback.
    // Used while approving, so two admins can't give the same student two passes at once.
    public boolean lockById(Connection con, int studentId) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement("SELECT student_id FROM students WHERE student_id = ? FOR UPDATE");
            ps.setInt(1, studentId);
            rs = ps.executeQuery();
            return rs.next();
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
    }

    // true if some OTHER student already has this roll number
    // (registration passes 0 as excludeStudentId because no student has id 0)
    public boolean rollNoExists(Connection con, String rollNo, int excludeStudentId) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement("SELECT 1 FROM students WHERE roll_no = ? AND student_id <> ?");
            ps.setString(1, rollNo);
            ps.setInt(2, excludeStudentId);
            rs = ps.executeQuery();
            return rs.next();
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
    }

    // used when a student registers, returns the new student_id
    public int insert(Connection con, Student s) throws SQLException {
        PreparedStatement ps = null;
        ResultSet keys = null;
        try {
            ps = con.prepareStatement("INSERT INTO students (user_id, roll_no, full_name, department, "
                    + "year_of_study, phone, email) VALUES (?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, s.getUserId());
            ps.setString(2, s.getRollNo());
            ps.setString(3, s.getFullName());
            ps.setString(4, s.getDepartment());
            ps.setInt(5, s.getYearOfStudy());
            ps.setString(6, s.getPhone());
            ps.setString(7, s.getEmail());
            ps.executeUpdate();
            keys = ps.getGeneratedKeys();
            keys.next();
            return keys.getInt(1);
        } finally {
            if (keys != null) keys.close();
            if (ps != null) ps.close();
        }
    }

    // edit profile - the roll number can't be changed, so it is not in the UPDATE
    public int update(Connection con, Student s) throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE students SET full_name = ?, department = ?, year_of_study = ?, "
                    + "phone = ?, email = ? WHERE student_id = ?");
            ps.setString(1, s.getFullName());
            ps.setString(2, s.getDepartment());
            ps.setInt(3, s.getYearOfStudy());
            ps.setString(4, s.getPhone());
            ps.setString(5, s.getEmail());
            ps.setInt(6, s.getStudentId());
            return ps.executeUpdate();
        } finally {
            if (ps != null) ps.close();
        }
    }

    private Student readStudent(ResultSet rs) throws SQLException {
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
