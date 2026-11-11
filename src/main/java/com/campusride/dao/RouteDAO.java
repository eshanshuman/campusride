package com.campusride.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.campusride.model.Route;

/** SQL for the routes table (joined with buses to get reg no and capacity). */
public class RouteDAO {

    private static final String SELECT_JOIN =
        "SELECT r.route_id, r.route_name, r.bus_id, r.is_active, b.reg_no, b.capacity "
      + "FROM routes r JOIN buses b ON b.bus_id = r.bus_id ";

    public List<Route> findAll(Connection con) throws SQLException {
        return query(con, SELECT_JOIN + "ORDER BY r.route_name", null);
    }

    /** Only routes students can apply for. */
    public List<Route> findActive(Connection con) throws SQLException {
        return query(con, SELECT_JOIN + "WHERE r.is_active = TRUE ORDER BY r.route_name", null);
    }

    public Route findById(Connection con, int routeId) throws SQLException {
        List<Route> list = query(con, SELECT_JOIN + "WHERE r.route_id = ?", routeId);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Same as findById but LOCKS the route row (and its bus row) until commit
     * or rollback. While we hold this lock, no other approval on the same route
     * can run its capacity check, so two admins can never fill the last seat twice.
     */
    public Route findByIdForUpdate(Connection con, int routeId) throws SQLException {
        List<Route> list = query(con, SELECT_JOIN + "WHERE r.route_id = ? FOR UPDATE", routeId);
        return list.isEmpty() ? null : list.get(0);
    }

    public void insert(Connection con, String routeName, int busId) throws SQLException {
        String sql = "INSERT INTO routes (route_name, bus_id, is_active) VALUES (?, ?, TRUE)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, routeName);
            ps.setInt(2, busId);
            ps.executeUpdate();
        }
    }

    /** Switch a route on/off. Inactive routes are hidden from students. */
    public int setActive(Connection con, int routeId, boolean active) throws SQLException {
        String sql = "UPDATE routes SET is_active = ? WHERE route_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setInt(2, routeId);
            return ps.executeUpdate();
        }
    }

    /** Deletes a route only if it has no stops (stops must be deleted first). */
    public int deleteIfUnused(Connection con, int routeId) throws SQLException {
        String sql = "DELETE FROM routes WHERE route_id = ? "
                   + "AND NOT EXISTS (SELECT 1 FROM route_stops s WHERE s.route_id = ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, routeId);
            ps.setInt(2, routeId);
            return ps.executeUpdate();
        }
    }

    /** Runs a SELECT with an optional single int parameter. */
    private List<Route> query(Connection con, String sql, Integer param) throws SQLException {
        List<Route> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (param != null) {
                ps.setInt(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Route r = new Route();
                    r.setRouteId(rs.getInt("route_id"));
                    r.setRouteName(rs.getString("route_name"));
                    r.setBusId(rs.getInt("bus_id"));
                    r.setActive(rs.getBoolean("is_active"));
                    r.setBusRegNo(rs.getString("reg_no"));
                    r.setCapacity(rs.getInt("capacity"));
                    list.add(r);
                }
            }
        }
        return list;
    }
}
