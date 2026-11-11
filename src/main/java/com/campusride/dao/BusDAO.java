package com.campusride.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.campusride.model.Bus;

/** SQL for the buses table. */
public class BusDAO {

    public List<Bus> findAll(Connection con) throws SQLException {
        String sql = "SELECT bus_id, reg_no, capacity FROM buses ORDER BY reg_no";
        return query(con, sql);
    }

    /** Buses not yet assigned to any route (a bus may serve only one route). */
    public List<Bus> findUnassigned(Connection con) throws SQLException {
        String sql = "SELECT b.bus_id, b.reg_no, b.capacity FROM buses b "
                   + "WHERE NOT EXISTS (SELECT 1 FROM routes r WHERE r.bus_id = b.bus_id) "
                   + "ORDER BY b.reg_no";
        return query(con, sql);
    }

    public void insert(Connection con, String regNo, int capacity) throws SQLException {
        String sql = "INSERT INTO buses (reg_no, capacity) VALUES (?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, regNo);
            ps.setInt(2, capacity);
            ps.executeUpdate();
        }
    }

    public int updateCapacity(Connection con, int busId, int capacity) throws SQLException {
        String sql = "UPDATE buses SET capacity = ? WHERE bus_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, capacity);
            ps.setInt(2, busId);
            return ps.executeUpdate();
        }
    }

    /**
     * Deletes a bus only if no route uses it. The NOT EXISTS makes the rule
     * visible in the SQL; the foreign key would also refuse it (error 1451).
     * Returns the number of rows deleted (0 means the bus is in use).
     */
    public int deleteIfUnused(Connection con, int busId) throws SQLException {
        String sql = "DELETE FROM buses WHERE bus_id = ? "
                   + "AND NOT EXISTS (SELECT 1 FROM routes r WHERE r.bus_id = ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, busId);
            ps.setInt(2, busId);
            return ps.executeUpdate();
        }
    }

    private List<Bus> query(Connection con, String sql) throws SQLException {
        List<Bus> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Bus b = new Bus();
                b.setBusId(rs.getInt("bus_id"));
                b.setRegNo(rs.getString("reg_no"));
                b.setCapacity(rs.getInt("capacity"));
                list.add(b);
            }
        }
        return list;
    }
}
