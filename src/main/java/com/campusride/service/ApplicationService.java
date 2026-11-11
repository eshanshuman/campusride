package com.campusride.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import com.campusride.dao.ApplicationDAO;
import com.campusride.dao.PassDAO;
import com.campusride.dao.RouteDAO;
import com.campusride.dao.StopDAO;
import com.campusride.model.Application;
import com.campusride.model.BusPass;
import com.campusride.model.Route;
import com.campusride.model.Stop;
import com.campusride.util.DBConnection;

/**
 * Student side of bus-pass applications: create (draft or submit), submit a
 * draft, delete a draft, and list own applications and passes.
 * (The admin decision - approve/reject - is in AdminService.)
 */
public class ApplicationService {

    /** A pass may be requested for at most one year. */
    private static final int MAX_DAYS = 366;

    private final ApplicationDAO appDAO = new ApplicationDAO();
    private final PassDAO passDAO = new PassDAO();
    private final StopDAO stopDAO = new StopDAO();
    private final RouteDAO routeDAO = new RouteDAO();

    /**
     * Creates a new application for the logged-in student.
     * submitNow = true  -> status SUBMITTED (goes to the admin)
     * submitNow = false -> status DRAFT     (student can submit or delete later)
     * Returns the new application id.
     */
    public int createApplication(Integer stopId, LocalDate from, LocalDate to, String remarks, boolean submitNow)
            throws ServiceException {
        int studentId = StudentService.currentStudentId();
        if (stopId == null) {
            throw new ServiceException("Please select a stop.");
        }
        checkDates(from, to);
        String note = (remarks == null || remarks.trim().isEmpty()) ? null : remarks.trim();
        if (note != null && note.length() > 255) {
            throw new ServiceException("Remarks must be at most 255 characters.");
        }

        try (Connection con = DBConnection.getConnection()) {
            if (submitNow) {
                checkCanSubmit(con, studentId, stopId, from, to, 0);
            } else if (stopDAO.findById(con, stopId) == null) {
                throw new ServiceException("The selected stop no longer exists.");
            }
            Application a = new Application();
            a.setStudentId(studentId);
            a.setStopId(stopId);
            a.setValidFrom(from);
            a.setValidTo(to);
            a.setStatus(submitNow ? "SUBMITTED" : "DRAFT");
            a.setRemarks(note);
            return appDAO.insert(con, a);
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    /** DRAFT -> SUBMITTED, after the same checks as a new submission. */
    public void submitDraft(int applicationId) throws ServiceException {
        int studentId = StudentService.currentStudentId();
        try (Connection con = DBConnection.getConnection()) {
            Application a = appDAO.findById(con, applicationId);
            if (a == null || a.getStudentId() != studentId) {
                throw new ServiceException("Application not found.");
            }
            if (!"DRAFT".equals(a.getStatus())) {
                throw new ServiceException("Only DRAFT applications can be submitted.");
            }
            checkDates(a.getValidFrom(), a.getValidTo());
            checkCanSubmit(con, studentId, a.getStopId(), a.getValidFrom(), a.getValidTo(), applicationId);
            if (appDAO.submitDraft(con, applicationId, studentId) != 1) {
                throw new ServiceException("This draft could not be submitted. Please refresh.");
            }
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    /** Deletes the student's own DRAFT. Submitted/approved/rejected ones are kept forever. */
    public void deleteDraft(int applicationId) throws ServiceException {
        int studentId = StudentService.currentStudentId();
        try (Connection con = DBConnection.getConnection()) {
            if (appDAO.deleteDraft(con, applicationId, studentId) != 1) {
                throw new ServiceException("Only your own DRAFT applications can be deleted.");
            }
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    public List<Application> getMyApplications() throws ServiceException {
        int studentId = StudentService.currentStudentId();
        try (Connection con = DBConnection.getConnection()) {
            return appDAO.findByStudent(con, studentId);
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    public List<BusPass> getMyPasses() throws ServiceException {
        int studentId = StudentService.currentStudentId();
        try (Connection con = DBConnection.getConnection()) {
            passDAO.expireOldPasses(con);   // so old passes show as EXPIRED
            return passDAO.findByStudent(con, studentId);
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        }
    }

    // ------------------------------------------------------------------ helpers

    /** valid_to after valid_from, start not in the past, at most one year long. */
    private void checkDates(LocalDate from, LocalDate to) throws ServiceException {
        Validation.dateRange(from, to);
        if (from.isBefore(LocalDate.now())) {
            throw new ServiceException("'Valid from' cannot be in the past.");
        }
        if (from.plusDays(MAX_DAYS).isBefore(to)) {
            throw new ServiceException("A pass can be requested for at most one year.");
        }
    }

    /**
     * Friendly early checks before an application reaches the admin.
     * The admin's approval transaction checks capacity and overlap AGAIN,
     * because things can change between submitting and approving.
     */
    private void checkCanSubmit(Connection con, int studentId, int stopId, LocalDate from, LocalDate to,
                                int excludeApplicationId) throws SQLException, ServiceException {
        Stop stop = stopDAO.findById(con, stopId);
        if (stop == null) {
            throw new ServiceException("The selected stop no longer exists.");
        }
        Route route = routeDAO.findById(con, stop.getRouteId());
        if (route == null || !route.isActive()) {
            throw new ServiceException("This route is not running at the moment. Please choose another route.");
        }
        if (passDAO.countActiveOverlapForStudent(con, studentId, from, to) > 0) {
            throw new ServiceException("You already have an ACTIVE pass for (part of) these dates.");
        }
        if (appDAO.countSubmittedOverlap(con, studentId, from, to, excludeApplicationId) > 0) {
            throw new ServiceException("You already have a submitted application for these dates. "
                    + "Please wait for the admin's decision.");
        }
        if (passDAO.countActiveOverlapOnRoute(con, route.getRouteId(), from, to) >= route.getCapacity()) {
            throw new ServiceException("Sorry, " + route.getRouteName() + " is full for these dates. "
                    + "Please choose another route or period.");
        }
    }
}
