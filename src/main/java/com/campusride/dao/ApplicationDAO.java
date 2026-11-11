package com.campusride.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.campusride.model.Application;

/**
 * SQL for the pass_applications table.
 * The route of an application is found with a JOIN through its stop
 * (pass_applications -> route_stops -> routes); it is never stored twice.
 */
public class ApplicationDAO {

    private static final String SELECT_JOIN =
        "SELECT a.application_id, a.student_id, a.stop_id, a.valid_from, a.valid_to, a.status, "
      + "       a.remarks, a.applied_at, a.reviewed_by, a.reviewed_at, a.admin_remarks, "
      + "       s.full_name, s.roll_no, st.stop_name, st.route_id, r.route_name "
      + "FROM pass_applications a "
      + "JOIN students    s  ON s.student_id = a.student_id "
      + "JOIN route_stops st ON st.stop_id   = a.stop_id "
      + "JOIN routes      r  ON r.route_id   = st.route_id ";

    /** All applications of one student, newest first. */
    public List<Application> findByStudent(Connection con, int studentId) throws SQLException {
        String sql = SELECT_JOIN + "WHERE a.student_id = ? ORDER BY a.applied_at DESC, a.application_id DESC";
        List<Application> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    /** All applications with a given status (e.g. SUBMITTED for the admin), oldest first. */
    public List<Application> findByStatus(Connection con, String status) throws SQLException {
        String sql = SELECT_JOIN + "WHERE a.status = ? ORDER BY a.applied_at, a.application_id";
        List<Application> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public Application findById(Connection con, int applicationId) throws SQLException {
        String sql = SELECT_JOIN + "WHERE a.application_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, applicationId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /**
     * Locks the application row and returns its CURRENT status (or null if gone).
     * Used inside the approval transaction to re-check that it is still SUBMITTED.
     */
    public String lockAndGetStatus(Connection con, int applicationId) throws SQLException {
        String sql = "SELECT status FROM pass_applications WHERE application_id = ? FOR UPDATE";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, applicationId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("status") : null;
            }
        }
    }

    /** INSERT a new application (status DRAFT or SUBMITTED). Returns the new id. */
    public int insert(Connection con, Application a) throws SQLException {
        String sql = "INSERT INTO pass_applications (student_id, stop_id, valid_from, valid_to, status, remarks) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, a.getStudentId());
            ps.setInt(2, a.getStopId());
            ps.setDate(3, Date.valueOf(a.getValidFrom()));
            ps.setDate(4, Date.valueOf(a.getValidTo()));
            ps.setString(5, a.getStatus());
            ps.setString(6, a.getRemarks());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    /** Turns the student's own DRAFT into SUBMITTED. Returns rows changed (0 = not a draft). */
    public int submitDraft(Connection con, int applicationId, int studentId) throws SQLException {
        String sql = "UPDATE pass_applications SET status = 'SUBMITTED', applied_at = NOW() "
                   + "WHERE application_id = ? AND student_id = ? AND status = 'DRAFT'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, applicationId);
            ps.setInt(2, studentId);
            return ps.executeUpdate();
        }
    }

    /** DELETE is allowed ONLY for the student's own DRAFT. Returns rows deleted. */
    public int deleteDraft(Connection con, int applicationId, int studentId) throws SQLException {
        String sql = "DELETE FROM pass_applications "
                   + "WHERE application_id = ? AND student_id = ? AND status = 'DRAFT'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, applicationId);
            ps.setInt(2, studentId);
            return ps.executeUpdate();
        }
    }

    /**
     * Admin decision: newStatus is APPROVED or REJECTED. The "AND status = 'SUBMITTED'"
     * means an application can be decided only once. Returns rows changed.
     */
    public int review(Connection con, int applicationId, String newStatus, int adminUserId, String adminRemarks)
            throws SQLException {
        String sql = "UPDATE pass_applications "
                   + "SET status = ?, reviewed_by = ?, reviewed_at = NOW(), admin_remarks = ? "
                   + "WHERE application_id = ? AND status = 'SUBMITTED'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, adminUserId);
            ps.setString(3, adminRemarks);
            ps.setInt(4, applicationId);
            return ps.executeUpdate();
        }
    }

    /**
     * How many SUBMITTED applications of this student overlap the dates
     * (used to stop a student sending the same request twice).
     */
    public int countSubmittedOverlap(Connection con, int studentId, LocalDate from, LocalDate to,
                                     int excludeApplicationId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM pass_applications "
                   + "WHERE student_id = ? AND status = 'SUBMITTED' AND application_id <> ? "
                   + "AND valid_from <= ? AND valid_to >= ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, excludeApplicationId);
            ps.setDate(3, Date.valueOf(to));
            ps.setDate(4, Date.valueOf(from));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private Application map(ResultSet rs) throws SQLException {
        Application a = new Application();
        a.setApplicationId(rs.getInt("application_id"));
        a.setStudentId(rs.getInt("student_id"));
        a.setStopId(rs.getInt("stop_id"));
        a.setValidFrom(rs.getDate("valid_from").toLocalDate());
        a.setValidTo(rs.getDate("valid_to").toLocalDate());
        a.setStatus(rs.getString("status"));
        a.setRemarks(rs.getString("remarks"));
        a.setAppliedAt(rs.getTimestamp("applied_at").toLocalDateTime());
        int reviewer = rs.getInt("reviewed_by");
        a.setReviewedBy(rs.wasNull() ? null : reviewer);     // keep NULL as null
        Timestamp reviewedAt = rs.getTimestamp("reviewed_at");
        a.setReviewedAt(reviewedAt == null ? null : reviewedAt.toLocalDateTime());
        a.setAdminRemarks(rs.getString("admin_remarks"));
        a.setStudentName(rs.getString("full_name"));
        a.setRollNo(rs.getString("roll_no"));
        a.setStopName(rs.getString("stop_name"));
        a.setRouteId(rs.getInt("route_id"));
        a.setRouteName(rs.getString("route_name"));
        return a;
    }
}
