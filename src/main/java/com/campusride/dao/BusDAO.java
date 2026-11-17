package com.campusride.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import com.campusride.model.Bus;

// SQL for the buses table
public class BusDAO {

    public Vector<Bus> findAll(Connection con) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        Vector<Bus> list = new Vector<Bus>();
        try {
            ps = con.prepareStatement("SELECT bus_id, reg_no, capacity FROM buses ORDER BY reg_no");
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(readBus(rs));
            }
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
        return list;
    }

    // buses that no route is using yet (a bus can only be on one route)
    public Vector<Bus> findUnassigned(Connection con) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        Vector<Bus> list = new Vector<Bus>();
        try {
            ps = con.prepareStatement("SELECT bus_id, reg_no, capacity FROM buses "
                    + "WHERE bus_id NOT IN (SELECT bus_id FROM routes) ORDER BY reg_no");
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(readBus(rs));
            }
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
        return list;
    }

    public void insert(Connection con, String regNo, int capacity) throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO buses (reg_no, capacity) VALUES (?, ?)");
            ps.setString(1, regNo);
            ps.setInt(2, capacity);
            ps.executeUpdate();
        } finally {
            if (ps != null) ps.close();
        }
    }

    public int updateCapacity(Connection con, int busId, int capacity) throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE buses SET capacity = ? WHERE bus_id = ?");
            ps.setInt(1, capacity);
            ps.setInt(2, busId);
            return ps.executeUpdate();
        } finally {
            if (ps != null) ps.close();
        }
    }

    // only deletes the bus if no route uses it. Returns 0 if it was not deleted.
    public int deleteIfUnused(Connection con, int busId) throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM buses WHERE bus_id = ? "
                    + "AND NOT EXISTS (SELECT 1 FROM routes WHERE routes.bus_id = ?)");
            ps.setInt(1, busId);
            ps.setInt(2, busId);
            return ps.executeUpdate();
        } finally {
            if (ps != null) ps.close();
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
