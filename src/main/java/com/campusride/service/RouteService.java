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

// route information for students: routes, stops, timings and seats left
public class RouteService {

    private RouteDAO routeDAO = new RouteDAO();
    private StopDAO stopDAO = new StopDAO();
    private PassDAO passDAO = new PassDAO();

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

    // seats left = bus capacity - ACTIVE passes on this route that overlap the dates.
    // We count every overlapping pass, which is simple and can never overbook the bus.
    public int getSeatsLeft(int routeId, LocalDate from, LocalDate to) throws ServiceException {
        Validation.dateRange(from, to);
        try (Connection con = DBConnection.getConnection()) {
            Route route = routeDAO.findById(con, routeId);
            if (route == null) {
                throw new ServiceException("This route no longer exists.");
            }
            int seatsUsed = passDAO.countActiveOverlapOnRoute(con, routeId, from, to);
            int seatsLeft = route.getCapacity() - seatsUsed;
            if (seatsLeft < 0) {
                seatsLeft = 0;
            }
            return seatsLeft;
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }
}
