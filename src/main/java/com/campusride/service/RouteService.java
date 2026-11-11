package com.campusride.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import com.campusride.dao.PassDAO;
import com.campusride.dao.RouteDAO;
import com.campusride.dao.StopDAO;
import com.campusride.model.Route;
import com.campusride.model.Stop;
import com.campusride.util.DBConnection;

/** Read-only route information for students (routes, stops, timings, seats). */
public class RouteService {

    private final RouteDAO routeDAO = new RouteDAO();
    private final StopDAO stopDAO = new StopDAO();
    private final PassDAO passDAO = new PassDAO();

    public List<Route> getActiveRoutes() throws ServiceException {
        try (Connection con = DBConnection.getConnection()) {
            return routeDAO.findActive(con);
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    public List<Stop> getStops(int routeId) throws ServiceException {
        try (Connection con = DBConnection.getConnection()) {
            return stopDAO.findByRoute(con, routeId);
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    /**
     * Seats left on a route for a period = bus capacity minus ACTIVE passes on
     * that route whose dates overlap the period. (We count every overlapping
     * pass, which is simple and always safe - it can never overbook.)
     */
    public int getSeatsLeft(int routeId, LocalDate from, LocalDate to) throws ServiceException {
        Validation.dateRange(from, to);
        try (Connection con = DBConnection.getConnection()) {
            Route route = routeDAO.findById(con, routeId);
            if (route == null) {
                throw new ServiceException("This route no longer exists.");
            }
            int used = passDAO.countActiveOverlapOnRoute(con, routeId, from, to);
            return Math.max(0, route.getCapacity() - used);
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }
}
