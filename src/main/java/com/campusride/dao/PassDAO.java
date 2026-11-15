package com.campusride.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.campusride.model.BusPass;

// SQL for the bus_passes table.
// Two date ranges overlap when  from1 <= to2  AND  to1 >= from2.
public class PassDAO {

    private static final String SELECT_PASSES =
        "SELECT p.*, s.full_name, s.roll_no, s.phone, r.route_name, b.reg_no, "
      + "       st.stop_name, st.stop_seq, st.pickup_time "
      + "FROM bus_passes p "
      + "JOIN pass_applications a ON a.application_id = p.application_id "
      + "JOIN students s     ON s.student_id = a.student_id "
      + "JOIN route_stops st ON st.stop_id = a.stop_id "
      + "JOIN routes r       ON r.route_id = st.route_id "
      + "JOIN buses b        ON b.bus_id = r.bus_id ";

    // how many ACTIVE passes on this route overlap the given dates (= seats used)
    public int countActiveOverlapOnRoute(Connection con, int routeId, LocalDate from, LocalDate to)
            throws SQLException {
        String sql = "SELECT COUNT(*) FROM bus_passes p "
                   + "JOIN pass_applications a ON a.application_id = p.application_id "
                   + "JOIN route_stops st ON st.stop_id = a.stop_id "
                   + "WHERE st.route_id = ? AND p.status = 'ACTIVE' "
                   + "AND p.valid_from <= ? AND p.valid_to >= ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, routeId);
            ps.setDate(2, Date.valueOf(to));
            ps.setDate(3, Date.valueOf(from));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    // how many ACTIVE passes this student has that overlap the given dates
    public int countActiveOverlapForStudent(Connection con, int studentId, LocalDate from, LocalDate to)
            throws SQLException {
        String sql = "SELECT COUNT(*) FROM bus_passes p "
                   + "JOIN pass_applications a ON a.application_id = p.application_id "
                   + "WHERE a.student_id = ? AND p.status = 'ACTIVE' "
                   + "AND p.valid_from <= ? AND p.valid_to >= ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setDate(2, Date.valueOf(to));
            ps.setDate(3, Date.valueOf(from));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    // Next pass number for the year, e.g. CR-2026-00003 if the biggest so far is CR-2026-00002.
    // If two admins get the same number at the same time, the UNIQUE key on pass_no stops the second one.
    public String nextPassNo(Connection con, int year) throws SQLException {
        String prefix = "CR-" + year + "-";
        // SUBSTRING(pass_no, 9) = the 5 digits after "CR-2026-"
        String sql = "SELECT MAX(CAST(SUBSTRING(pass_no, 9) AS UNSIGNED)) FROM bus_passes WHERE pass_no LIKE ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int biggest = rs.getInt(1);      // NULL (no passes this year yet) comes back as 0
                int nextNumber = biggest + 1;
                return prefix + String.format("%05d", nextNumber);   // 3 -> "00003"
            }
        }
    }

    // new ACTIVE pass, returns the new pass_id
    public int insert(Connection con, int applicationId, String passNo, LocalDate from, LocalDate to)
            throws SQLException {
        String sql = "INSERT INTO bus_passes (application_id, pass_no, valid_from, valid_to, status) "
                   + "VALUES (?, ?, ?, ?, 'ACTIVE')";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, applicationId);
            ps.setString(2, passNo);
            ps.setDate(3, Date.valueOf(from));
            ps.setDate(4, Date.valueOf(to));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    // all passes of one student, newest first
    public List<BusPass> findByStudent(Connection con, int studentId) throws SQLException {
        String sql = SELECT_PASSES + "WHERE a.student_id = ? ORDER BY p.issued_at DESC";
        List<BusPass> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(readPass(rs));
                }
            }
        }
        return list;
    }

    // Passenger list of a route: ACTIVE passes that haven't ended yet,
    // sorted by stop so the driver can read it from the first stop.
    public List<BusPass> findPassengers(Connection con, int routeId) throws SQLException {
        String sql = SELECT_PASSES
                   + "WHERE st.route_id = ? AND p.status = 'ACTIVE' AND p.valid_to >= CURDATE() "
                   + "ORDER BY st.stop_seq, s.full_name";
        List<BusPass> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, routeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(readPass(rs));
                }
            }
        }
        return list;
    }

    // ACTIVE -> CANCELLED, which frees the seat. We never delete passes.
    public int cancel(Connection con, int passId) throws SQLException {
        String sql = "UPDATE bus_passes SET status = 'CANCELLED' WHERE pass_id = ? AND status = 'ACTIVE'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, passId);
            return ps.executeUpdate();
        }
    }

    // passes whose end date has passed become EXPIRED
    public int expireOldPasses(Connection con) throws SQLException {
        String sql = "UPDATE bus_passes SET status = 'EXPIRED' WHERE status = 'ACTIVE' AND valid_to < CURDATE()";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            return ps.executeUpdate();
        }
    }

    private BusPass readPass(ResultSet rs) throws SQLException {
        BusPass p = new BusPass();
        p.setPassId(rs.getInt("pass_id"));
        p.setApplicationId(rs.getInt("application_id"));
        p.setPassNo(rs.getString("pass_no"));
        p.setValidFrom(rs.getDate("valid_from").toLocalDate());
        p.setValidTo(rs.getDate("valid_to").toLocalDate());
        p.setStatus(rs.getString("status"));
        p.setIssuedAt(rs.getTimestamp("issued_at").toLocalDateTime());
        p.setStudentName(rs.getString("full_name"));
        p.setRollNo(rs.getString("roll_no"));
        p.setPhone(rs.getString("phone"));
        p.setRouteName(rs.getString("route_name"));
        p.setBusRegNo(rs.getString("reg_no"));
        p.setStopName(rs.getString("stop_name"));
        p.setStopSeq(rs.getInt("stop_seq"));
        p.setPickupTime(rs.getTime("pickup_time").toLocalTime());
        return p;
    }
}
