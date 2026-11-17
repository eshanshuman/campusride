package com.campusride.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalTime;
import java.util.Vector;

import com.campusride.model.Stop;

// SQL for the route_stops table
public class StopDAO {

    // stops of one route, in the order the bus reaches them
    public Vector<Stop> findByRoute(Connection con, int routeId) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        Vector<Stop> list = new Vector<Stop>();
        try {
            ps = con.prepareStatement("SELECT * FROM route_stops WHERE route_id = ? ORDER BY stop_seq");
            ps.setInt(1, routeId);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(readStop(rs));
            }
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
        return list;
    }

    public Stop findById(Connection con, int stopId) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement("SELECT * FROM route_stops WHERE stop_id = ?");
            ps.setInt(1, stopId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return readStop(rs);
            }
            return null;
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
    }

    public void insert(Connection con, int routeId, String name, int seq, LocalTime time) throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO route_stops (route_id, stop_name, stop_seq, pickup_time) "
                    + "VALUES (?, ?, ?, ?)");
            ps.setInt(1, routeId);
            ps.setString(2, name);
            ps.setInt(3, seq);
            ps.setTime(4, Time.valueOf(time));
            ps.executeUpdate();
        } finally {
            if (ps != null) ps.close();
        }
    }

    // only deletes the stop if no application uses it. Returns 0 if not deleted.
    public int deleteIfUnused(Connection con, int stopId) throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM route_stops WHERE stop_id = ? "
                    + "AND NOT EXISTS (SELECT 1 FROM pass_applications WHERE pass_applications.stop_id = ?)");
            ps.setInt(1, stopId);
            ps.setInt(2, stopId);
            return ps.executeUpdate();
        } finally {
            if (ps != null) ps.close();
        }
    }

    private Stop readStop(ResultSet rs) throws SQLException {
        Stop s = new Stop();
        s.setStopId(rs.getInt("stop_id"));
        s.setRouteId(rs.getInt("route_id"));
        s.setStopName(rs.getString("stop_name"));
        s.setStopSeq(rs.getInt("stop_seq"));
        s.setPickupTime(rs.getTime("pickup_time").toLocalTime());
        return s;
    }
}
