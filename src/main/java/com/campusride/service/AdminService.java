package com.campusride.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import com.campusride.dao.ApplicationDAO;
import com.campusride.dao.BusDAO;
import com.campusride.dao.PassDAO;
import com.campusride.dao.RouteDAO;
import com.campusride.dao.StopDAO;
import com.campusride.dao.StudentDAO;
import com.campusride.model.Application;
import com.campusride.model.Bus;
import com.campusride.model.BusPass;
import com.campusride.model.Route;
import com.campusride.model.Stop;
import com.campusride.util.DBConnection;
import com.campusride.util.Session;

// Everything the transport admin does:
// approve/reject, passenger lists, and adding/removing buses, routes and stops.
public class AdminService {

    private ApplicationDAO appDAO = new ApplicationDAO();
    private PassDAO passDAO = new PassDAO();
    private RouteDAO routeDAO = new RouteDAO();
    private StudentDAO studentDAO = new StudentDAO();
    private BusDAO busDAO = new BusDAO();
    private StopDAO stopDAO = new StopDAO();

    // ---------- Approvals ----------

    public List<Application> getPendingApplications() throws ServiceException {
        checkAdmin();
        try (Connection con = DBConnection.getConnection()) {
            passDAO.expireOldPasses(con);
            return appDAO.findByStatus(con, "SUBMITTED");
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    // numbers for the "Capacity check" box
    public CapacityInfo getCapacityInfo(Application app) throws ServiceException {
        checkAdmin();
        try (Connection con = DBConnection.getConnection()) {
            Route route = routeDAO.findById(con, app.getRouteId());
            if (route == null) {
                throw new ServiceException("The route of this application no longer exists.");
            }
            int seatsUsed = passDAO.countActiveOverlapOnRoute(con, route.getRouteId(), app.getValidFrom(), app.getValidTo());
            int studentPasses = passDAO.countActiveOverlapForStudent(con, app.getStudentId(), app.getValidFrom(), app.getValidTo());
            boolean studentHasPass = studentPasses > 0;
            return new CapacityInfo(route.getRouteName(), route.getBusRegNo(), route.getCapacity(), seatsUsed, studentHasPass);
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    // Approve = ONE transaction. Either all of it happens or none of it.
    // Returns the new pass number, e.g. "CR-2026-00003".
    public String approve(int applicationId, String adminRemarks) throws ServiceException {
        checkAdmin();
        int adminUserId = Session.getUser().getUserId();
        String remarks = cleanRemarks(adminRemarks);

        try (Connection con = DBConnection.getConnection()) {
            // READ COMMITTED: after we get the locks, our COUNT queries see passes
            // that another admin committed just a moment ago
            con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            // 1. start the transaction
            con.setAutoCommit(false);
            try {
                Application app = appDAO.findById(con, applicationId);
                if (app == null) {
                    throw new ServiceException("Application not found.");
                }

                // 2. lock the route so two admins can't take the same seat
                Route route = routeDAO.findByIdForUpdate(con, app.getRouteId());
                if (route == null) {
                    throw new ServiceException("The route of this application no longer exists.");
                }
                // 3. lock the student so they can't get two passes at the same time
                studentDAO.lockById(con, app.getStudentId());

                // 4. check everything again now that we hold the locks
                String status = appDAO.lockAndGetStatus(con, applicationId);
                if (!"SUBMITTED".equals(status)) {
                    throw new ServiceException("This application is already " + status + ".");
                }
                if (!route.isActive()) {
                    throw new ServiceException(route.getRouteName() + " is not active. Activate it or reject the application.");
                }
                LocalDate from = app.getValidFrom();
                LocalDate to = app.getValidTo();
                if (passDAO.countActiveOverlapForStudent(con, app.getStudentId(), from, to) > 0) {
                    throw new ServiceException(app.getStudentName() + " already has an ACTIVE pass overlapping these dates.");
                }
                int seatsUsed = passDAO.countActiveOverlapOnRoute(con, route.getRouteId(), from, to);
                if (seatsUsed >= route.getCapacity()) {
                    throw new ServiceException("Bus " + route.getBusRegNo() + " is full for this period ("
                            + seatsUsed + " of " + route.getCapacity() + " seats taken). Cannot approve.");
                }

                // 5. mark the application APPROVED
                int rows = appDAO.review(con, applicationId, "APPROVED", adminUserId, remarks);
                if (rows != 1) {
                    throw new ServiceException("This application was changed by someone else. Please refresh.");
                }

                // 6. issue the pass
                String passNo = passDAO.nextPassNo(con, LocalDate.now().getYear());
                passDAO.insert(con, applicationId, passNo, from, to);

                // 7. save everything
                con.commit();
                return passNo;
            } catch (ServiceException e) {
                con.rollback();   // undo the update/insert and release the locks
                throw e;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    // only a SUBMITTED application can be rejected, and the admin must give a reason
    public void reject(int applicationId, String adminRemarks) throws ServiceException {
        checkAdmin();
        String remarks = Validation.required(adminRemarks, "Reason for rejection", 255);
        try (Connection con = DBConnection.getConnection()) {
            int rows = appDAO.review(con, applicationId, "REJECTED", Session.getUser().getUserId(), remarks);
            if (rows != 1) {
                throw new ServiceException("This application is no longer waiting for a decision. Please refresh.");
            }
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    // ---------- Passenger lists ----------

    public List<Route> getAllRoutes() throws ServiceException {
        checkAdmin();
        try (Connection con = DBConnection.getConnection()) {
            return routeDAO.findAll(con);
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    public List<BusPass> getPassengers(int routeId) throws ServiceException {
        checkAdmin();
        try (Connection con = DBConnection.getConnection()) {
            passDAO.expireOldPasses(con);
            return passDAO.findPassengers(con, routeId);
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    // cancelling frees the seat; passes are never deleted
    public void cancelPass(int passId) throws ServiceException {
        checkAdmin();
        try (Connection con = DBConnection.getConnection()) {
            int rows = passDAO.cancel(con, passId);
            if (rows != 1) {
                throw new ServiceException("Only ACTIVE passes can be cancelled.");
            }
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    // ---------- Buses, routes and stops ----------

    public List<Bus> getAllBuses() throws ServiceException {
        checkAdmin();
        try (Connection con = DBConnection.getConnection()) {
            return busDAO.findAll(con);
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    public List<Bus> getUnassignedBuses() throws ServiceException {
        checkAdmin();
        try (Connection con = DBConnection.getConnection()) {
            return busDAO.findUnassigned(con);
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    public void addBus(String regNo, String capacityText) throws ServiceException {
        checkAdmin();
        String reg = Validation.required(regNo, "Registration number", 15).toUpperCase();
        int capacity = readPositiveNumber(capacityText, "Capacity");
        try (Connection con = DBConnection.getConnection()) {
            busDAO.insert(con, reg, capacity);
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    // capacity can't go below the number of people who already have ACTIVE passes on that bus
    public void changeCapacity(int busId, String capacityText) throws ServiceException {
        checkAdmin();
        int capacity = readPositiveNumber(capacityText, "Capacity");
        try (Connection con = DBConnection.getConnection()) {
            List<Route> routes = routeDAO.findAll(con);
            for (Route r : routes) {
                if (r.getBusId() == busId) {
                    int passengers = passDAO.findPassengers(con, r.getRouteId()).size();
                    if (capacity < passengers) {
                        throw new ServiceException("Capacity cannot be less than the " + passengers
                                + " passengers holding ACTIVE passes on " + r.getRouteName() + ".");
                    }
                }
            }
            busDAO.updateCapacity(con, busId, capacity);
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    public void deleteBus(int busId) throws ServiceException {
        checkAdmin();
        try (Connection con = DBConnection.getConnection()) {
            int rows = busDAO.deleteIfUnused(con, busId);
            if (rows != 1) {
                throw new ServiceException("This bus is assigned to a route, so it cannot be deleted.");
            }
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    public void addRoute(String routeName, Bus bus) throws ServiceException {
        checkAdmin();
        String name = Validation.required(routeName, "Route name", 60);
        if (bus == null) {
            throw new ServiceException("Please choose a free bus for the route (add a bus first if none is free).");
        }
        try (Connection con = DBConnection.getConnection()) {
            routeDAO.insert(con, name, bus.getBusId());
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    public void setRouteActive(int routeId, boolean active) throws ServiceException {
        checkAdmin();
        try (Connection con = DBConnection.getConnection()) {
            routeDAO.setActive(con, routeId, active);
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    public void deleteRoute(int routeId) throws ServiceException {
        checkAdmin();
        try (Connection con = DBConnection.getConnection()) {
            int rows = routeDAO.deleteIfUnused(con, routeId);
            if (rows != 1) {
                throw new ServiceException("This route still has stops. Delete its stops first "
                        + "(or simply mark the route inactive).");
            }
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    public List<Stop> getStops(int routeId) throws ServiceException {
        checkAdmin();
        try (Connection con = DBConnection.getConnection()) {
            return stopDAO.findByRoute(con, routeId);
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    // pickup time is typed like 07:45 (24-hour clock)
    public void addStop(Route route, String stopName, String seqText, String timeText) throws ServiceException {
        checkAdmin();
        if (route == null) {
            throw new ServiceException("Please select a route first.");
        }
        String name = Validation.required(stopName, "Stop name", 80);
        int seq = readPositiveNumber(seqText, "Stop order");
        LocalTime time;
        try {
            time = LocalTime.parse(Validation.required(timeText, "Pickup time", 5));
        } catch (DateTimeParseException e) {
            throw new ServiceException("Pickup time must look like 07:45 (24-hour clock).");
        }
        try (Connection con = DBConnection.getConnection()) {
            stopDAO.insert(con, route.getRouteId(), name, seq, time);
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    public void deleteStop(int stopId) throws ServiceException {
        checkAdmin();
        try (Connection con = DBConnection.getConnection()) {
            int rows = stopDAO.deleteIfUnused(con, stopId);
            if (rows != 1) {
                throw new ServiceException("Students have applied for this stop, so it cannot be deleted.");
            }
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    // ---------- small helpers ----------

    private void checkAdmin() throws ServiceException {
        if (Session.getUser() == null || !Session.getUser().isAdmin()) {
            throw new ServiceException("Only the transport admin can do this.");
        }
    }

    // turns text like "40" into a number bigger than 0
    private int readPositiveNumber(String text, String fieldName) throws ServiceException {
        String value = Validation.required(text, fieldName, 6);
        int number;
        try {
            number = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ServiceException(fieldName + " must be a whole number greater than 0.");
        }
        if (number <= 0) {
            throw new ServiceException(fieldName + " must be a whole number greater than 0.");
        }
        return number;
    }

    // empty remarks are saved as NULL
    private String cleanRemarks(String remarks) throws ServiceException {
        if (remarks == null || remarks.trim().isEmpty()) {
            return null;
        }
        if (remarks.trim().length() > 255) {
            throw new ServiceException("Remarks must be at most 255 characters.");
        }
        return remarks.trim();
    }
}
