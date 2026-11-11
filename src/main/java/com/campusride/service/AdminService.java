package com.campusride.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
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

/**
 * Everything the transport admin does: approve/reject (with the capacity
 * transaction), passenger lists, and maintaining buses, routes and stops.
 */
public class AdminService {

    private final ApplicationDAO appDAO = new ApplicationDAO();
    private final PassDAO passDAO = new PassDAO();
    private final RouteDAO routeDAO = new RouteDAO();
    private final StudentDAO studentDAO = new StudentDAO();
    private final BusDAO busDAO = new BusDAO();
    private final StopDAO stopDAO = new StopDAO();

    // ================================================================ approvals

    /** Applications waiting for a decision. Also marks old passes EXPIRED first. */
    public List<Application> getPendingApplications() throws ServiceException {
        requireAdmin();
        try (Connection con = DBConnection.getConnection()) {
            passDAO.expireOldPasses(con);
            return appDAO.findByStatus(con, "SUBMITTED");
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    /** Numbers for the capacity panel of one application. */
    public CapacityInfo getCapacityInfo(Application app) throws ServiceException {
        requireAdmin();
        try (Connection con = DBConnection.getConnection()) {
            Route route = routeDAO.findById(con, app.getRouteId());
            if (route == null) {
                throw new ServiceException("The route of this application no longer exists.");
            }
            int used = passDAO.countActiveOverlapOnRoute(con, route.getRouteId(), app.getValidFrom(), app.getValidTo());
            boolean overlap = passDAO.countActiveOverlapForStudent(
                    con, app.getStudentId(), app.getValidFrom(), app.getValidTo()) > 0;
            return new CapacityInfo(route.getRouteName(), route.getBusRegNo(), route.getCapacity(), used, overlap);
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    /**
     * APPROVE = ONE TRANSACTION. Either everything below happens, or nothing:
     *
     *  1. setAutoCommit(false)                         - start the transaction
     *  2. SELECT route ... FOR UPDATE                  - lock the route (one approval per route at a time)
     *  3. SELECT student ... FOR UPDATE                - lock the student
     *  4. re-check: application still SUBMITTED, route active,
     *     student has no overlapping ACTIVE pass,
     *     ACTIVE passes on route overlapping the dates < bus capacity
     *  5. UPDATE application -> APPROVED
     *  6. INSERT bus_passes with a new unique pass_no
     *  7. commit                                       - or rollback on ANY failure
     *
     * We use READ COMMITTED isolation so that, once we hold the locks, every
     * COUNT sees the latest committed passes (another admin's approval that
     * finished a moment ago is counted).
     *
     * Returns the new pass number, e.g. "CR-2026-00003".
     */
    public String approve(int applicationId, String adminRemarks) throws ServiceException {
        requireAdmin();
        int adminUserId = Session.getUser().getUserId();
        String remarks = cleanRemarks(adminRemarks);

        try (Connection con = DBConnection.getConnection()) {
            con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            con.setAutoCommit(false);
            try {
                Application app = appDAO.findById(con, applicationId);
                if (app == null) {
                    throw new ServiceException("Application not found.");
                }

                // ---- 2 + 3: take the locks (always route first, then student) ----
                Route route = routeDAO.findByIdForUpdate(con, app.getRouteId());
                if (route == null) {
                    throw new ServiceException("The route of this application no longer exists.");
                }
                studentDAO.lockById(con, app.getStudentId());

                // ---- 4: re-check every rule while holding the locks ----
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
                    throw new ServiceException(app.getStudentName()
                            + " already has an ACTIVE pass overlapping these dates.");
                }
                int used = passDAO.countActiveOverlapOnRoute(con, route.getRouteId(), from, to);
                if (used >= route.getCapacity()) {
                    throw new ServiceException("Bus " + route.getBusRegNo() + " is full for this period ("
                            + used + " of " + route.getCapacity() + " seats taken). Cannot approve.");
                }

                // ---- 5: application -> APPROVED ----
                if (appDAO.review(con, applicationId, "APPROVED", adminUserId, remarks) != 1) {
                    throw new ServiceException("This application was changed by someone else. Please refresh.");
                }

                // ---- 6: issue the pass ----
                String passNo = passDAO.nextPassNo(con, LocalDate.now().getYear());
                passDAO.insert(con, applicationId, passNo, from, to);

                // ---- 7: make it permanent ----
                con.commit();
                return passNo;
            } catch (ServiceException | SQLException e) {
                con.rollback();      // undo the UPDATE/INSERT, release the locks
                throw e;
            }
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    /** REJECT: only a SUBMITTED application, and a reason is required. */
    public void reject(int applicationId, String adminRemarks) throws ServiceException {
        requireAdmin();
        String remarks = Validation.required(adminRemarks, "Reason for rejection", 255);
        try (Connection con = DBConnection.getConnection()) {
            if (appDAO.review(con, applicationId, "REJECTED", Session.getUser().getUserId(), remarks) != 1) {
                throw new ServiceException("This application is no longer waiting for a decision. Please refresh.");
            }
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    // ========================================================= passenger lists

    public List<Route> getAllRoutes() throws ServiceException {
        requireAdmin();
        try (Connection con = DBConnection.getConnection()) {
            return routeDAO.findAll(con);
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    /** Current and upcoming ACTIVE passes on a route, in stop order. */
    public List<BusPass> getPassengers(int routeId) throws ServiceException {
        requireAdmin();
        try (Connection con = DBConnection.getConnection()) {
            passDAO.expireOldPasses(con);
            return passDAO.findPassengers(con, routeId);
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    /** Cancels an ACTIVE pass (frees the seat). Passes are never deleted. */
    public void cancelPass(int passId) throws ServiceException {
        requireAdmin();
        try (Connection con = DBConnection.getConnection()) {
            if (passDAO.cancel(con, passId) != 1) {
                throw new ServiceException("Only ACTIVE passes can be cancelled.");
            }
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    // ===================================================== buses / routes / stops

    public List<Bus> getAllBuses() throws ServiceException {
        requireAdmin();
        try (Connection con = DBConnection.getConnection()) {
            return busDAO.findAll(con);
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    public List<Bus> getUnassignedBuses() throws ServiceException {
        requireAdmin();
        try (Connection con = DBConnection.getConnection()) {
            return busDAO.findUnassigned(con);
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    public void addBus(String regNo, String capacityText) throws ServiceException {
        requireAdmin();
        String reg = Validation.required(regNo, "Registration number", 15).toUpperCase();
        int capacity = parsePositiveInt(capacityText, "Capacity");
        try (Connection con = DBConnection.getConnection()) {
            busDAO.insert(con, reg, capacity);
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    /**
     * Changes a bus's capacity. It may not go below the number of passengers
     * who currently hold ACTIVE passes on that bus's route.
     */
    public void changeCapacity(int busId, String capacityText) throws ServiceException {
        requireAdmin();
        int capacity = parsePositiveInt(capacityText, "Capacity");
        try (Connection con = DBConnection.getConnection()) {
            for (Route r : routeDAO.findAll(con)) {
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
        requireAdmin();
        try (Connection con = DBConnection.getConnection()) {
            if (busDAO.deleteIfUnused(con, busId) != 1) {
                throw new ServiceException("This bus is assigned to a route, so it cannot be deleted.");
            }
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    public void addRoute(String routeName, Bus bus) throws ServiceException {
        requireAdmin();
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
        requireAdmin();
        try (Connection con = DBConnection.getConnection()) {
            routeDAO.setActive(con, routeId, active);
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    public void deleteRoute(int routeId) throws ServiceException {
        requireAdmin();
        try (Connection con = DBConnection.getConnection()) {
            if (routeDAO.deleteIfUnused(con, routeId) != 1) {
                throw new ServiceException("This route still has stops. Delete its stops first "
                        + "(or simply mark the route inactive).");
            }
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    public List<Stop> getStops(int routeId) throws ServiceException {
        requireAdmin();
        try (Connection con = DBConnection.getConnection()) {
            return stopDAO.findByRoute(con, routeId);
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    /** timeText must look like 07:45 (24-hour clock). */
    public void addStop(Route route, String stopName, String seqText, String timeText) throws ServiceException {
        requireAdmin();
        if (route == null) {
            throw new ServiceException("Please select a route first.");
        }
        String name = Validation.required(stopName, "Stop name", 80);
        int seq = parsePositiveInt(seqText, "Stop order");
        LocalTime time;
        try {
            time = LocalTime.parse(Validation.required(timeText, "Pickup time", 5));
        } catch (java.time.format.DateTimeParseException e) {
            throw new ServiceException("Pickup time must look like 07:45 (24-hour clock).");
        }
        try (Connection con = DBConnection.getConnection()) {
            stopDAO.insert(con, route.getRouteId(), name, seq, time);
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    public void deleteStop(int stopId) throws ServiceException {
        requireAdmin();
        try (Connection con = DBConnection.getConnection()) {
            if (stopDAO.deleteIfUnused(con, stopId) != 1) {
                throw new ServiceException("Students have applied for this stop, so it cannot be deleted.");
            }
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    // ------------------------------------------------------------------ helpers

    private void requireAdmin() throws ServiceException {
        if (Session.getUser() == null || !Session.getUser().isAdmin()) {
            throw new ServiceException("Only the transport admin can do this.");
        }
    }

    private int parsePositiveInt(String text, String fieldName) throws ServiceException {
        String v = Validation.required(text, fieldName, 6);
        try {
            int n = Integer.parseInt(v);
            if (n <= 0) {
                throw new NumberFormatException();
            }
            return n;
        } catch (NumberFormatException e) {
            throw new ServiceException(fieldName + " must be a whole number greater than 0.");
        }
    }

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
