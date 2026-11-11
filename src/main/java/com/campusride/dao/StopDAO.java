package com.campusride.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.campusride.model.Stop;

/** SQL for the route_stops table. */
public class StopDAO {

    private static final String COLUMNS = "stop_id, route_id, stop_name, stop_seq, pickup_time";

    /** Stops of one route in travel order. */
    public List<Stop> findByRoute(Connection con, int routeId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM route_stops WHERE route_id = ? ORDER BY stop_seq";
        List<Stop> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, routeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public Stop findById(Connection con, int stopId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM route_stops WHERE stop_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, stopId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public void insert(Connection con, int routeId, String name, int seq, LocalTime time) throws SQLException {
        String sql = "INSERT INTO route_stops (route_id, stop_name, stop_seq, pickup_time) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, routeId);
            ps.setString(2, name);
            ps.setInt(3, seq);
            ps.setTime(4, Time.valueOf(time));
            ps.executeUpdate();
        }
    }

    /** Deletes a stop only if no application refers to it. Returns rows deleted. */
    public int deleteIfUnused(Connection con, int stopId) throws SQLException {
        String sql = "DELETE FROM route_stops WHERE stop_id = ? "
                   + "AND NOT EXISTS (SELECT 1 FROM pass_applications a WHERE a.stop_id = ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, stopId);
            ps.setInt(2, stopId);
            return ps.executeUpdate();
        }
    }

    private Stop map(ResultSet rs) throws SQLException {
        Stop s = new Stop();
        s.setStopId(rs.getInt("stop_id"));
        s.setRouteId(rs.getInt("route_id"));
        s.setStopName(rs.getString("stop_name"));
        s.setStopSeq(rs.getInt("stop_seq"));
        s.setPickupTime(rs.getTime("pickup_time").toLocalTime());
        return s;
    }
}
