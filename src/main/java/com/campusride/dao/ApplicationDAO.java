package com.campusride.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Vector;

import com.campusride.model.Application;

// SQL for the pass_applications table.
// We find the route of an application by joining application -> stop -> route.
public class ApplicationDAO {

    private static final String SELECT_APPS =
        "SELECT a.*, s.full_name, s.roll_no, st.stop_name, st.route_id, r.route_name "
      + "FROM pass_applications a "
      + "JOIN students s     ON s.student_id = a.student_id "
      + "JOIN route_stops st ON st.stop_id = a.stop_id "
      + "JOIN routes r       ON r.route_id = st.route_id ";

    // all applications of one student, newest first
    public Vector<Application> findByStudent(Connection con, int studentId) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        Vector<Application> list = new Vector<Application>();
        try {
            ps = con.prepareStatement(SELECT_APPS + "WHERE a.student_id = ? "
                    + "ORDER BY a.applied_at DESC, a.application_id DESC");
            ps.setInt(1, studentId);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(readApplication(rs));
            }
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
        return list;
    }

    // e.g. all SUBMITTED applications for the admin, oldest first
    public Vector<Application> findByStatus(Connection con, String status) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        Vector<Application> list = new Vector<Application>();
        try {
            ps = con.prepareStatement(SELECT_APPS + "WHERE a.status = ? ORDER BY a.applied_at, a.application_id");
            ps.setString(1, status);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(readApplication(rs));
            }
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
        return list;
    }

    public Application findById(Connection con, int applicationId) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement(SELECT_APPS + "WHERE a.application_id = ?");
            ps.setInt(1, applicationId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return readApplication(rs);
            }
            return null;
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
    }

    // Locks the application row and gives back its status right now.
    // Used during approval to check it is still SUBMITTED.
    public String lockAndGetStatus(Connection con, int applicationId) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement("SELECT status FROM pass_applications WHERE application_id = ? FOR UPDATE");
            ps.setInt(1, applicationId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("status");
            }
            return null;
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
    }

    // new application, status is DRAFT or SUBMITTED. Returns the new id.
    public int insert(Connection con, Application a) throws SQLException {
        PreparedStatement ps = null;
        ResultSet keys = null;
        try {
            ps = con.prepareStatement("INSERT INTO pass_applications (student_id, stop_id, valid_from, valid_to, "
                    + "status, remarks) VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, a.getStudentId());
            ps.setInt(2, a.getStopId());
            ps.setDate(3, Date.valueOf(a.getValidFrom()));
            ps.setDate(4, Date.valueOf(a.getValidTo()));
            ps.setString(5, a.getStatus());
            ps.setString(6, a.getRemarks());
            ps.executeUpdate();
            keys = ps.getGeneratedKeys();
            keys.next();
            return keys.getInt(1);
        } finally {
            if (keys != null) keys.close();
            if (ps != null) ps.close();
        }
    }

    // DRAFT -> SUBMITTED. Returns 0 if it was not this student's draft.
    public int submitDraft(Connection con, int applicationId, int studentId) throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE pass_applications SET status = 'SUBMITTED', applied_at = NOW() "
                    + "WHERE application_id = ? AND student_id = ? AND status = 'DRAFT'");
            ps.setInt(1, applicationId);
            ps.setInt(2, studentId);
            return ps.executeUpdate();
        } finally {
            if (ps != null) ps.close();
        }
    }

    // Students can only delete their own DRAFT. Returns 0 if nothing was deleted.
    public int deleteDraft(Connection con, int applicationId, int studentId) throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM pass_applications "
                    + "WHERE application_id = ? AND student_id = ? AND status = 'DRAFT'");
            ps.setInt(1, applicationId);
            ps.setInt(2, studentId);
            return ps.executeUpdate();
        } finally {
            if (ps != null) ps.close();
        }
    }

    // Admin decision, newStatus is APPROVED or REJECTED.
    // "AND status = 'SUBMITTED'" means an application can only be decided once.
    public int review(Connection con, int applicationId, String newStatus, int adminUserId, String adminRemarks)
            throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE pass_applications "
                    + "SET status = ?, reviewed_by = ?, reviewed_at = NOW(), admin_remarks = ? "
                    + "WHERE application_id = ? AND status = 'SUBMITTED'");
            ps.setString(1, newStatus);
            ps.setInt(2, adminUserId);
            ps.setString(3, adminRemarks);
            ps.setInt(4, applicationId);
            return ps.executeUpdate();
        } finally {
            if (ps != null) ps.close();
        }
    }

    // How many SUBMITTED applications this student has for overlapping dates.
    // Two date ranges overlap when  from1 <= to2  AND  to1 >= from2.
    public int countSubmittedOverlap(Connection con, int studentId, LocalDate from, LocalDate to,
                                     int excludeApplicationId) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement("SELECT COUNT(*) FROM pass_applications "
                    + "WHERE student_id = ? AND status = 'SUBMITTED' AND application_id <> ? "
                    + "AND valid_from <= ? AND valid_to >= ?");
            ps.setInt(1, studentId);
            ps.setInt(2, excludeApplicationId);
            ps.setDate(3, Date.valueOf(to));
            ps.setDate(4, Date.valueOf(from));
            rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
    }

    private Application readApplication(ResultSet rs) throws SQLException {
        Application a = new Application();
        a.setApplicationId(rs.getInt("application_id"));
        a.setStudentId(rs.getInt("student_id"));
        a.setStopId(rs.getInt("stop_id"));
        a.setValidFrom(rs.getDate("valid_from").toLocalDate());
        a.setValidTo(rs.getDate("valid_to").toLocalDate());
        a.setStatus(rs.getString("status"));
        a.setRemarks(rs.getString("remarks"));
        a.setAppliedAt(rs.getTimestamp("applied_at").toLocalDateTime());

        // getInt gives 0 for NULL, so we check wasNull() to keep it as null
        int reviewer = rs.getInt("reviewed_by");
        if (rs.wasNull()) {
            a.setReviewedBy(null);
        } else {
            a.setReviewedBy(reviewer);
        }
        Timestamp reviewedAt = rs.getTimestamp("reviewed_at");
        if (reviewedAt != null) {
            a.setReviewedAt(reviewedAt.toLocalDateTime());
        }
        a.setAdminRemarks(rs.getString("admin_remarks"));

        a.setStudentName(rs.getString("full_name"));
        a.setRollNo(rs.getString("roll_no"));
        a.setStopName(rs.getString("stop_name"));
        a.setRouteId(rs.getInt("route_id"));
        a.setRouteName(rs.getString("route_name"));
        return a;
    }
}
