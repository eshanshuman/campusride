package com.campusride.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import com.campusride.model.Route;

// SQL for the routes table. We JOIN buses to also get the reg no and capacity.
public class RouteDAO {

    private static final String SELECT_ROUTES =
        "SELECT r.route_id, r.route_name, r.bus_id, r.is_active, b.reg_no, b.capacity "
      + "FROM routes r JOIN buses b ON b.bus_id = r.bus_id ";

    public Vector<Route> findAll(Connection con) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        Vector<Route> list = new Vector<Route>();
        try {
            ps = con.prepareStatement(SELECT_ROUTES + "ORDER BY r.route_name");
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(readRoute(rs));
            }
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
        return list;
    }

    // only the routes that are running (students only see these)
    public Vector<Route> findActive(Connection con) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        Vector<Route> list = new Vector<Route>();
        try {
            ps = con.prepareStatement(SELECT_ROUTES + "WHERE r.is_active = TRUE ORDER BY r.route_name");
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(readRoute(rs));
            }
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
        return list;
    }

    public Route findById(Connection con, int routeId) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement(SELECT_ROUTES + "WHERE r.route_id = ?");
            ps.setInt(1, routeId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return readRoute(rs);
            }
            return null;
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
    }

    // Same as findById, but FOR UPDATE locks the route row (and its bus row) until commit/rollback.
    // While one admin holds this lock, another admin approving on the same route has to wait,
    // so two people can never take the last seat.
    public Route findByIdForUpdate(Connection con, int routeId) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement(SELECT_ROUTES + "WHERE r.route_id = ? FOR UPDATE");
            ps.setInt(1, routeId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return readRoute(rs);
            }
            return null;
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
    }

    public void insert(Connection con, String routeName, int busId) throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO routes (route_name, bus_id, is_active) VALUES (?, ?, TRUE)");
            ps.setString(1, routeName);
            ps.setInt(2, busId);
            ps.executeUpdate();
        } finally {
            if (ps != null) ps.close();
        }
    }

    public int setActive(Connection con, int routeId, boolean active) throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE routes SET is_active = ? WHERE route_id = ?");
            ps.setBoolean(1, active);
            ps.setInt(2, routeId);
            return ps.executeUpdate();
        } finally {
            if (ps != null) ps.close();
        }
    }

    // only deletes a route that has no stops left. Returns 0 if not deleted.
    public int deleteIfUnused(Connection con, int routeId) throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM routes WHERE route_id = ? "
                    + "AND NOT EXISTS (SELECT 1 FROM route_stops WHERE route_stops.route_id = ?)");
            ps.setInt(1, routeId);
            ps.setInt(2, routeId);
            return ps.executeUpdate();
        } finally {
            if (ps != null) ps.close();
        }
    }

    private Route readRoute(ResultSet rs) throws SQLException {
        Route r = new Route();
        r.setRouteId(rs.getInt("route_id"));
        r.setRouteName(rs.getString("route_name"));
        r.setBusId(rs.getInt("bus_id"));
        r.setActive(rs.getBoolean("is_active"));
        r.setBusRegNo(rs.getString("reg_no"));
        r.setCapacity(rs.getInt("capacity"));
        return r;
    }
}
