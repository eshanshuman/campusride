package com.campusride.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.campusride.model.Bus;

// SQL for the buses table
public class BusDAO {

    public List<Bus> findAll(Connection con) throws SQLException {
        String sql = "SELECT bus_id, reg_no, capacity FROM buses ORDER BY reg_no";
        List<Bus> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(readBus(rs));
            }
        }
        return list;
    }

    // buses that no route is using yet (a bus can only be on one route)
    public List<Bus> findUnassigned(Connection con) throws SQLException {
        String sql = "SELECT bus_id, reg_no, capacity FROM buses "
                   + "WHERE bus_id NOT IN (SELECT bus_id FROM routes) ORDER BY reg_no";
        List<Bus> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(readBus(rs));
            }
        }
        return list;
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

    // only deletes the bus if no route uses it. Returns 0 if it was not deleted.
    public int deleteIfUnused(Connection con, int busId) throws SQLException {
        String sql = "DELETE FROM buses WHERE bus_id = ? "
                   + "AND NOT EXISTS (SELECT 1 FROM routes WHERE routes.bus_id = ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, busId);
            ps.setInt(2, busId);
            return ps.executeUpdate();
        }
    }

    private Bus readBus(ResultSet rs) throws SQLException {
        Bus b = new Bus();
        b.setBusId(rs.getInt("bus_id"));
        b.setRegNo(rs.getString("reg_no"));
        b.setCapacity(rs.getInt("capacity"));
        return b;
    }
}
